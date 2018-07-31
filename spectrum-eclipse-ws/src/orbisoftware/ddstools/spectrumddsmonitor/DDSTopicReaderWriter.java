/*
 *  Spectrum DDS data monitor tool
 *
 *  Copyright (C) 2011 Harlan Murphy
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

package orbisoftware.ddstools.spectrumddsmonitor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DDSTopicReaderWriter extends Thread implements
      PropertyChangeListener {

   private Class<?> dWriterClass = null;
   private Class<?> dWriterImpl = null;

   private Class<?> seqHolderClass = null;
   private Class<?> dReaderClass = null;
   private Class<?> dReaderImpl = null;

   // Memory for seqHolderRingBuffer is owned by the DDS
   // middleware. MAX_SEQ_HOLDERS determines the number
   // of sample sequence holders are in simultaneous use.
   private final int MAX_SEQ_HOLDERS = 10;
   private Object seqHolderRingBuffer[] = new Object[MAX_SEQ_HOLDERS];
   private DDS.SampleInfoSeqHolder sampleInfoSeq[] = new DDS.SampleInfoSeqHolder[MAX_SEQ_HOLDERS];
   private int seqHolderIndex = 0;

   private Object dReader = null;
   private Object dWriter = null;

   private DDS.DomainParticipant dp = null;
   private DDS.ReadCondition readCondition = null;
   private DDS.ConditionSeqHolder conditions = null;

   private DDS.WaitSet waitSet = null;
   private DDS.Duration_t waitDuration = null;
   private DDS.Time_t writeTime = null;

   private final String DATA_WRITER_HELPER = "DataWriterHelper";
   private final String DATA_WRITER_IMPL = "DataWriterImpl";
   private final String WRITE_METHOD = "write_w_timestamp";

   private final String DATA_READER_HELPER = "DataReaderHelper";
   private final String DATA_READER_IMPL = "DataReaderImpl";
   private final String NARROW_METHOD = "narrow";

   private final String CREATE_READCONDITION_METHOD = "create_readcondition";
   private final String READ_METHOD = "read";
   private final String TAKE_METHOD = "take";

   private final String SEQ_HOLDER = "SeqHolder";
   private final String SEQ_VALUE_FIELD = "value";
   private final String TYPE_SUPPORT = "TypeSupport";

   private final String GET_TYPE_NAME_METHOD = "get_type_name";
   private final String REGISTER_TYPE_METHOD = "register_type";
   private final String RETURN_LOAN_METHOD = "return_loan";

   private PropertyChangeSupport readEventPCS;
   private PropertyChangeSupport dataLoggerEventPCS;
   private PropertyChangeSupport sessionCaptureEventPCS;

   private BlockingQueue<Object> writeSamplesQueue = null;
   private DDSTopic ddsTopic = null;
   private Field valueField;
   private String topicDataType;

   private int status;
   private boolean initialized;
   private boolean shutdownReq;
   private long initialStartTime;

   /** Create new instance */
   public DDSTopicReaderWriter(DDSTopic ddsTopic) {
      this.ddsTopic = ddsTopic;
      initialized = false;
      shutdownReq = false;
      initialStartTime = System.currentTimeMillis();
      ddsTopic.topicReaderWriterFinished = false;
      writeSamplesQueue = new LinkedBlockingQueue<Object>();
   }

   /** Establish new DDS Connection for topic */
   public Boolean DDSConnect(String partition, String topicDataType) {

      DDS.DomainParticipantFactory dpf;
      DDS.DomainParticipantQosHolder dpQos;

      DDS.Subscriber subscriber;
      DDS.SubscriberQosHolder sQos;

      DDS.Publisher publisher;
      DDS.PublisherQosHolder pQos;

      DDS.DataReaderQosHolder drQos;
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
      sQos = new DDS.SubscriberQosHolder();
      pQos = new DDS.PublisherQosHolder();

      dp = dpf.create_participant(null, dpQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      dp.get_default_topic_qos(tQos);
      dp.get_default_subscriber_qos(sQos);
      dp.get_default_publisher_qos(pQos);

      switch (ddsTopic.topicQos) {

      case DDS_QOS_PROFILE_2:
         tQos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
         tQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
         tQos.value.history.depth = 1;
         break;

      case DDS_QOS_PROFILE_3:
         tQos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
         tQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
         tQos.value.history.depth = 30;
         break;

      case DDS_QOS_PROFILE_1:
      default:
         break;
      }

      // Create Subscriber
      sQos.value.partition.name = new String[1];
      if (partition != null)
         sQos.value.partition.name[0] = new String(partition);
      else
         sQos.value.partition.name[0] = "";
      subscriber = dp.create_subscriber(sQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      // Create Publisher
      pQos.value.partition.name = new String[1];
      if (partition != null)
         pQos.value.partition.name[0] = new String(partition);
      else
         pQos.value.partition.name[0] = "";
      publisher = dp.create_publisher(pQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      conditions = new DDS.ConditionSeqHolder();

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

      // Create datareader
      drQos = new DDS.DataReaderQosHolder();
      subscriber.get_default_datareader_qos(drQos);

      switch (ddsTopic.topicQos) {

      case DDS_QOS_PROFILE_2:
         drQos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
         drQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
         drQos.value.history.depth = 1;
         break;

      case DDS_QOS_PROFILE_3:
         drQos.value.reliability.kind = DDS.ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
         drQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
         drQos.value.history.depth = 30;
         break;

      case DDS_QOS_PROFILE_1:
      default:
         break;
      }

      DDS.DataReader udr = subscriber.create_datareader(topic, drQos.value,
            null, DDS.STATUS_MASK_NONE.value);

      if (udr != null) {

         // Narrow the data reader to the appropriate datatype
         try {
            dReaderClass = Class.forName(topicDataType + DATA_READER_HELPER);
            Class<?>[] methodTypeParams = { Object.class };
            Method narrowMethod = dReaderClass.getMethod(NARROW_METHOD,
                  methodTypeParams);
            Object[] methodParams = { udr };
            dReader = narrowMethod.invoke(dReaderClass, methodParams);
         } catch (Exception e) {
            System.err
                  .println("ERROR: Data Reader narrow method invocation failed");
            errorEncountered = true;
         }
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
         dwQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
         dwQos.value.history.depth = 30;
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

      // Create readCondition
      try {
         dReaderImpl = Class.forName(topicDataType + DATA_READER_IMPL);
         Class<?>[] methodTypeParams = { int.class, int.class, int.class };
         Method readConditionMethod = dReaderImpl.getMethod(
               CREATE_READCONDITION_METHOD, methodTypeParams);
         Object[] anySampleStateMethodParams = { DDS.ANY_SAMPLE_STATE.value,
               DDS.ANY_VIEW_STATE.value, DDS.ALIVE_INSTANCE_STATE.value };
         readCondition = (DDS.ReadCondition) readConditionMethod.invoke(
               dReader, anySampleStateMethodParams);
      } catch (Exception e) {
         System.err.println("ERROR: ReadConditionMethod invocation failed");
         errorEncountered = true;
      }

      // Create waitSet
      int milliSeconds = TopicManager.getInstance().GetReadConditionTimeout();
      waitDuration = new DDS.Duration_t(milliSeconds / 1000,
            (milliSeconds % 1000) * 1000000);

      writeTime = new DDS.Time_t(0, 0);

      waitSet = new DDS.WaitSet();
      status = waitSet.attach_condition(readCondition);

      if (status != DDS.RETCODE_OK.value) {
         System.err.println("ERROR: Cannot attach ReadCondition to WaitSet.");
         errorEncountered = true;
      }

      try {
         seqHolderClass = Class.forName(topicDataType + SEQ_HOLDER);
         for (int i = 0; i < MAX_SEQ_HOLDERS; i++) {
            seqHolderRingBuffer[i] = seqHolderClass.newInstance();
            sampleInfoSeq[i] = new DDS.SampleInfoSeqHolder();
         }
         valueField = seqHolderClass.getDeclaredField(SEQ_VALUE_FIELD);
      } catch (Exception e) {
         System.err.println("ERROR: SequenceHolder instantiation failed");
         errorEncountered = true;
      }

      // Check for connection problems
      if (errorEncountered)
         System.err.println("ERROR: DDS Connection failed");
      else
         initialized = true;

      // Spawn a new DDSReadEventHandler and DDSDataLogger thread
      if (initialized) {

         readEventPCS = new PropertyChangeSupport(this);
         ddsTopic.readEventHandler = new DDSReadEventHandler(ddsTopic);
         readEventPCS.addPropertyChangeListener(ddsTopic.readEventHandler);
         ddsTopic.readEventHandler.start();

         dataLoggerEventPCS = new PropertyChangeSupport(this);
         ddsTopic.dataLogger = new DDSDataLogger(ddsTopic);
         dataLoggerEventPCS.addPropertyChangeListener(ddsTopic.dataLogger);
         ddsTopic.dataLogger.start();

         sessionCaptureEventPCS = new PropertyChangeSupport(this);
         ddsTopic.sessionCapture = new DDSSessionCapture(ddsTopic);
         sessionCaptureEventPCS
               .addPropertyChangeListener(ddsTopic.sessionCapture);
         ddsTopic.sessionCapture.start();
      }

      return initialized;
   }

   /** Main thread execution */
   public void run() {

      int writeSamplesQueueSize;

      while (!ddsTopic.topicReaderWriterFinished) {

         if (seqHolderIndex == MAX_SEQ_HOLDERS)
            seqHolderIndex = 0;

         try {
            status = waitSet._wait(conditions, waitDuration);
         } catch (Exception e) {
         }

         synchronized (writeSamplesQueue) {
            writeSamplesQueueSize = writeSamplesQueue.size();
         }

         if (writeSamplesQueueSize > 0)
            writeSamples();

         // Shutdown if requested
         if (shutdownReq) {
            shutdown();
         } else {

            // Invoke readWCondition to read samples
            try {
               Class<?>[] methodTypeParams = { seqHolderClass,
                     DDS.SampleInfoSeqHolder.class, int.class, int.class,
                     int.class, int.class };

               if (ddsTopic.dataReaderMode == DDSTopic.DataReaderMode.READ) {

                  Method readMethod = dReaderImpl.getMethod(READ_METHOD,
                        methodTypeParams);
                  Object[] methodParams = {
                        seqHolderRingBuffer[seqHolderIndex],
                        sampleInfoSeq[seqHolderIndex],
                        DDS.LENGTH_UNLIMITED.value,
                        DDS.NOT_READ_SAMPLE_STATE.value,
                        DDS.ANY_VIEW_STATE.value,
                        DDS.ALIVE_INSTANCE_STATE.value };
                  status = (Integer) readMethod.invoke(dReader, methodParams);
               } else if (ddsTopic.dataReaderMode == DDSTopic.DataReaderMode.TAKE) {

                  Method takeMethod = dReaderImpl.getMethod(TAKE_METHOD,
                        methodTypeParams);
                  Object[] methodParams = {
                        seqHolderRingBuffer[seqHolderIndex],
                        sampleInfoSeq[seqHolderIndex],
                        DDS.LENGTH_UNLIMITED.value, DDS.ANY_SAMPLE_STATE.value,
                        DDS.ANY_VIEW_STATE.value,
                        DDS.ALIVE_INSTANCE_STATE.value };
                  status = (Integer) takeMethod.invoke(dReader, methodParams);
               }
            } catch (Exception e) {
               System.err.println("ERROR: Read method invocation failed");
            }

            if (status == DDS.RETCODE_OK.value) {

               boolean validData = false;

               DDSSamples ddsSamples = new DDSSamples();
               ddsSamples.sampleReadTime = System.currentTimeMillis()
                     - initialStartTime;
               ddsSamples.seqHolderName = new String(topicDataType + SEQ_HOLDER);
               ddsSamples.seqHolder = seqHolderRingBuffer[seqHolderIndex];

               // Verify the data is valid
               // (eliminates invalid sample on datawriter close)
               if (sampleInfoSeq[seqHolderIndex].value.length > 0) {

                  validData = true;

                  for (int i = 0; i < sampleInfoSeq[seqHolderIndex].value.length; i++) {

                     if (!sampleInfoSeq[seqHolderIndex].value[i].valid_data)
                        validData = false;
                  }
               }

               if (validData) {

                  if (ddsTopic.autoUpdate) {
                     readEventPCS.firePropertyChange("ddsReadEvent", 0,
                           ddsSamples);
                  }

                  if (TopicManager.getInstance().GetDataCaptureMode() == TopicManager.DataCaptureMode.DATA_LOGGER) {
                     dataLoggerEventPCS.firePropertyChange(
                           "ddsDataLoggerEvent", 0, ddsSamples);
                  } else if (TopicManager.getInstance().GetDataCaptureMode() == TopicManager.DataCaptureMode.SESSION_CAPTURE) {
                     sessionCaptureEventPCS.firePropertyChange(
                           "ddsSessionCaptureEvent", 0, ddsSamples);
                  }
               }
            }

            try {
               Thread.sleep(TopicManager.getInstance()
                     .GetReadConditionTimeout());
            } catch (InterruptedException e) {
            }
         }

         seqHolderIndex++;
      }
   }

   /** Handle ddsWriteEvent */
   public void propertyChange(PropertyChangeEvent evt) {

      if (evt.getPropertyName().toString().equals("ddsWriteEvent")) {
         synchronized (writeSamplesQueue) {
            writeSamplesQueue.add(evt.getNewValue());
         }
      }
   }

   /** Write out samples to DDS */
   private void writeSamples() {

      List<Object> samplesList = new LinkedList<Object>();

      synchronized (writeSamplesQueue) {
         writeSamplesQueue.drainTo(samplesList);
      }

      // Write out samples
      for (Object samples : samplesList) {

         Object val = null;
         try {
            val = valueField.get(samples);
         } catch (Exception e) {
         }
         int len = Array.getLength(val);

         // Process each sample in seqHolder
         for (int i = 0; i < len; i++) {

            Object dataClassObj = Array.get(val, i);

            // Invoke write method to write sample
            try {
               Class<?>[] methodTypeParams = { dataClassObj.getClass(),
                     long.class, writeTime.getClass() };
               Method writeMethod = dWriterImpl.getMethod(WRITE_METHOD,
                     methodTypeParams);
               Object[] methodParams = { dataClassObj, DDS.HANDLE_NIL.value,
                     writeTime };
               status = (Integer) writeMethod.invoke(dWriter, methodParams);
            } catch (Exception e) {
               System.err.println("ERROR: DataWriter write failed");
            }
         }
      }
   }

   /** Initiate shutdown request */
   public void shutdownReq() {
      shutdownReq = true;
   }

   /** Shutdown the thread */
   private void shutdown() {

      // Stop the Read Event Handler thread
      if (ddsTopic.readEventHandler != null)
         ddsTopic.readEventHandler.shutdownReq();

      // Return loaned sequenceHolder buffer
      try {
         Class<?>[] methodTypeParams = { seqHolderClass,
               DDS.SampleInfoSeqHolder.class };
         Method returnLoan = dReaderImpl.getMethod(RETURN_LOAN_METHOD,
               methodTypeParams);
         for (int i = 0; i < MAX_SEQ_HOLDERS; i++) {
            Object[] methodParams = { seqHolderRingBuffer[seqHolderIndex],
                  sampleInfoSeq[seqHolderIndex] };
            status = (Integer) returnLoan.invoke(dReader, methodParams);
         }
      } catch (Exception e) {
         System.err.println("ERROR: returnLoan failed");
      }

      ddsTopic.topicReaderWriterFinished = true;
   }
}
