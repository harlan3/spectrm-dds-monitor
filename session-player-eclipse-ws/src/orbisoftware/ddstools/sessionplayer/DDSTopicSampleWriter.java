/*
 *  Spectrum DDS Session Player
 *
 *  Copyright (C) 2012 Harlan Murphy
 *  Orbis Software - orbisoftware@gmail.com
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */

package orbisoftware.ddstools.sessionplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DDSTopicSampleWriter extends Thread implements
      PlaybackEventListener {

   private enum ThreadMode {
      Idle, Running
   }

   private Class<?> dWriterClass = null;
   private Class<?> dWriterImpl = null;

   private Object dWriter = null;

   private DDS.DomainParticipant dp = null;

   private final String DATA_WRITER_HELPER = "DataWriterHelper";
   private final String DATA_WRITER_IMPL = "DataWriterImpl";
   private final String WRITE_METHOD = "write";

   private final String NARROW_METHOD = "narrow";
   private final String TYPE_SUPPORT = "TypeSupport";

   private final String GET_TYPE_NAME_METHOD = "get_type_name";
   private final String REGISTER_TYPE_METHOD = "register_type";

   private final String SEQUENCE_CLASS_SUFFIX = "SeqHolder";
   private final String SEQ_VALUE_FIELD = "value";

   private DDSTopic ddsTopic;
   private Field valueField;
   private String topicDataType;

   @SuppressWarnings("unused")
   private int status;
   private boolean initialized;
   private boolean shutdownReq;

   private ThreadMode threadMode;
   private long commandedStartTime;
   private long playerElapsedTime;
   private boolean timeSynced;

   private int currentSample;
   private int numberSamples;

   private SessionPropertiesFile sessionPropertiesFile;
   private SampleManifest sampleManifest;

   private RandomAccessFile jsonSamplesRAF;

   /** Create new instance */
   public DDSTopicSampleWriter(DDSTopic ddsTopic) {

      this.ddsTopic = ddsTopic;
      initialized = false;
      shutdownReq = false;
      threadMode = ThreadMode.Idle;

      sessionPropertiesFile = SessionPropertiesFile.getInstance();
      sampleManifest = new SampleManifest();
      sampleManifest
            .loadSessionFile(sessionPropertiesFile.sessionPropertiesDirectory
                  + File.separator + ddsTopic.name + ".man");

      try {
         jsonSamplesRAF = new RandomAccessFile(
               sessionPropertiesFile.sessionPropertiesDirectory
                     + File.separator + ddsTopic.name + ".json", "r");
      } catch (FileNotFoundException e) {
      }

      ddsTopic.topicWriterShutdown = false;

      PlaybackEventManager.getInstance().addPlaybackEventListener(this);
   }

   /** Establish new DDS Connection for topic */
   public Boolean DDSConnect(String partition, String topicDataType) {

      DDS.DomainParticipantFactory dpf;
      DDS.DomainParticipantQosHolder dpQos;

      DDS.Publisher publisher;
      DDS.PublisherQosHolder pQos;

      DDS.DataWriterQosHolder dwQos;

      DDS.Topic topic;
      DDS.TopicQosHolder tQos;
      String topicDataTypeNS = null;

      boolean errorEncountered = false;
      this.topicDataType = topicDataType;

      // Create Participant and define Qos params
      dpf = DDS.DomainParticipantFactory.get_instance();
      dpQos = new DDS.DomainParticipantQosHolder();
      dpf.get_default_participant_qos(dpQos);

      tQos = new DDS.TopicQosHolder();
      pQos = new DDS.PublisherQosHolder();

      dp = dpf.create_participant(null, dpQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      dp.get_default_topic_qos(tQos);
      dp.get_default_publisher_qos(pQos);

      switch (ddsTopic.topicQos) {

         case DDS_QOS_PROFILE_2:
            tQos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
            tQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
            tQos.value.history.depth = 1;
            break;

         case DDS_QOS_PROFILE_3:
            tQos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
            tQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
            tQos.value.history.depth = 30;
            tQos.value.resource_limits.max_samples = 30;
            tQos.value.resource_limits.max_samples_per_instance = 30;
            break;

         case DDS_QOS_PROFILE_1:
         default:
            break;
      }

      // Create Publisher
      pQos.value.partition.name = new String[1];
      if (partition != null)
         pQos.value.partition.name[0] = new String(partition);
      else
         pQos.value.partition.name[0] = "";
      publisher = dp.create_publisher(pQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      // Register type support
      try {
         Class<?> typeSupportClass = Class
               .forName(topicDataType + TYPE_SUPPORT);
         Object typeSupport = typeSupportClass.newInstance();
         Class<?>[] arrGetTypeNameTypeParams = {};
         Method getTypeName = typeSupportClass.getMethod(GET_TYPE_NAME_METHOD,
               arrGetTypeNameTypeParams);
         Object[] arrGetTypeNameParams = {};
         topicDataTypeNS = (String) getTypeName.invoke(typeSupport,
               arrGetTypeNameParams);
         Class<?>[] arrRegisterTypeTypeParams = { DDS.DomainParticipant.class,
               String.class };
         Method registerType = typeSupportClass.getMethod(REGISTER_TYPE_METHOD,
               arrRegisterTypeTypeParams);
         Object[] arrRegisterTypeParams = { dp, topicDataTypeNS };
         status = (Integer) registerType.invoke(typeSupport,
               arrRegisterTypeParams);
      } catch (Exception e) {
         System.err.println("ERROR: register type support invocation failed");
         errorEncountered = true;
      }

      // Create the topic if it doesn't already exist
      topic = dp.find_topic(ddsTopic.name, DDS.DURATION_ZERO.value);
      if (topic == null) {
         topic = dp.create_topic(ddsTopic.name, topicDataTypeNS, tQos.value,
               null, DDS.STATUS_MASK_NONE.value);
      }

      dwQos = new DDS.DataWriterQosHolder();
      publisher.get_default_datawriter_qos(dwQos);

      switch (ddsTopic.topicQos) {

         case DDS_QOS_PROFILE_2:
            dwQos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
            dwQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
            dwQos.value.history.depth = 1;
            break;

         case DDS_QOS_PROFILE_3:
            dwQos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
            dwQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
            dwQos.value.history.depth = 30;
            dwQos.value.resource_limits.max_samples = 30;
            dwQos.value.resource_limits.max_samples_per_instance = 30;
            break;

         case DDS_QOS_PROFILE_1:
         default:
            break;
      }

      DDS.DataWriter udw = publisher.create_datawriter(topic, dwQos.value,
            null, DDS.STATUS_MASK_NONE.value);

      if (udw != null) {

         // Narrow the data writer to the appropriate datatype
         try {
            dWriterClass = Class.forName(topicDataType + DATA_WRITER_HELPER);
            dWriterImpl = Class.forName(topicDataType + DATA_WRITER_IMPL);
            Class<?>[] methodTypeParams = { Object.class };
            Method narrowMethod = dWriterClass.getMethod(NARROW_METHOD,
                  methodTypeParams);
            Object[] methodParams = { udw };
            dWriter = narrowMethod.invoke(dWriterClass, methodParams);
         } catch (Exception e) {
            System.err
                  .println("ERROR: Data Writer narrow method invocation failed");
            errorEncountered = true;
         }
      }

      // Check for connection problems
      if (errorEncountered)
         System.err.println("ERROR: DDS Connection failed");
      else
         initialized = true;

      return initialized;
   }

   /** Main thread execution */
   public void run() {

      while (!ddsTopic.topicWriterShutdown) {

         try {

            if (threadMode == ThreadMode.Running) {

               // this code fast-forwards through the manifest, to
               // timesync playback time up to the commanded
               // start time event. This allows playback from any
               // point in the recorded session
               if (!timeSynced) {

                  SampleManifestEntry sampleManifestEntry;
                  long sampleElapsedTime;
                  boolean sampleFound = false;

                  currentSample = 0;
                  numberSamples = (int) sampleManifest.getNumberSamples();

                  while (true) {

                     if (numberSamples > currentSample) {

                        sampleManifestEntry = sampleManifest
                              .getSample(currentSample);
                        sampleElapsedTime = sampleManifestEntry.sampleTimeStamp
                              - sessionPropertiesFile.sessionStartTime;
                     } else
                        break;

                     if (sampleElapsedTime >= playerElapsedTime) {
                        sampleFound = true;
                        break;
                     }
                     currentSample++;
                  }

                  if (sampleFound)
                     timeSynced = true;
               } /* if not timeSynced */

               if (timeSynced) {

                  SampleManifestEntry sampleManifestEntry = sampleManifest
                        .getSample(currentSample);

                  long sampleElapsedTime = sampleManifestEntry.sampleTimeStamp
                        - sessionPropertiesFile.sessionStartTime;
                  long playbackElapsedTime = System.currentTimeMillis()
                        - (commandedStartTime - playerElapsedTime);
                  long threadSleepTime = sampleElapsedTime
                        - playbackElapsedTime;

                  // Sleep for delay between samples
                  if (threadSleepTime > 0)
                     sleep(threadSleepTime);
                  else
                     sleep(10);

                  writeSamples(readJSON(sampleManifestEntry));
                  currentSample++;

                  // Switch thread to idle state if out of samples
                  if (currentSample == numberSamples)
                     threadMode = ThreadMode.Idle;
               }

            } else if (threadMode == ThreadMode.Idle) {

               timeSynced = false;
            }

            // Shutdown if requested
            if (shutdownReq)
               shutdown();

            // Sleep to save conserve cpu cycles
            if (threadMode == ThreadMode.Idle)
               Thread.sleep(100);

         } catch (Exception e3) {

            System.out.println("Exception in " + ddsTopic.name + " thread");
            e3.printStackTrace();
         }
      }
   }

   /** Read JSON entry */
   private String readJSON(SampleManifestEntry sampleManifestEntry) {

      byte[] byteBuffer = new byte[sampleManifestEntry.sampleSize];

      try {
         jsonSamplesRAF.seek(sampleManifestEntry.sampleFilePos);
         jsonSamplesRAF
               .readFully(byteBuffer, 0, sampleManifestEntry.sampleSize);
      } catch (IOException e) {
         System.err.println("ERROR: readJson failed");
      }

      return new String(byteBuffer);
   }

   /** Write samples to DDS */
   private void writeSamples(String jsonSample) {

      Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .serializeSpecialFloatingPointValues().create();

      try {
         Class<?> dataClass = Class.forName(topicDataType);
         Class<?> sequenceClass = Class.forName(topicDataType
               + SEQUENCE_CLASS_SUFFIX);
         valueField = sequenceClass.getDeclaredField(SEQ_VALUE_FIELD);

         Object sequenceObj = gson.fromJson(jsonSample, sequenceClass);
         Object arrayValObj = valueField.get(sequenceObj);
         Object dataClassObj = Array.get(arrayValObj, 0);
         int len = Array.getLength(arrayValObj);

         // Process each sample
         for (int i = 0; i < len; ++i) {

            dataClassObj = Array.get(arrayValObj, i);

            Class<?>[] methodTypeParams = { dataClass, long.class };
            Method writeMethod = dWriterImpl.getMethod(WRITE_METHOD,
                  methodTypeParams);
            Object[] methodParams = { dataClassObj, DDS.HANDLE_NIL.value };
            status = (Integer) writeMethod.invoke(dWriter, methodParams);
         }
      } catch (Exception e) {
         System.err.println("ERROR: DataWriter write failed");
      }
   }

   /** Initiate shutdown request */
   public void shutdownReq() {

      try {
         jsonSamplesRAF.close();
      } catch (IOException e) {
      }

      PlaybackEventManager.getInstance().removePlaybackEventListener(this);
      shutdownReq = true;
   }

   /** Shutdown the thread */
   private void shutdown() {

      ddsTopic.publisherAttached = false;
      ddsTopic.topicWriterShutdown = true;
   }

   /** Process new playback event */
   public void newPlaybackEvent(PlaybackEvent newPlaybackEvent) {

      if (newPlaybackEvent.getPlaybackEventType() == PlaybackEvent.PlaybackEventType.StartEvent) {

         threadMode = ThreadMode.Running;
         commandedStartTime = newPlaybackEvent.getCommandedStartTime();
         playerElapsedTime = newPlaybackEvent.getElapsedTime();

      } else if (newPlaybackEvent.getPlaybackEventType() == PlaybackEvent.PlaybackEventType.StopEvent) {

         threadMode = ThreadMode.Idle;
         commandedStartTime = newPlaybackEvent.getCommandedStartTime();
         playerElapsedTime = newPlaybackEvent.getElapsedTime();
      }
   }
}
