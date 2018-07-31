/*
 * This software is OSI Certified Open Source Software
 * 
 * The MIT License (MIT)
 * Copyright (C) 2012 Harlan Murphy
 * Orbis Software - orbisoftware@gmail.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */

package orbisoftware.ddstools.spectrumddsmonitor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class DDSDataAccessObj<DDSDataType, DDSSeqCont> {

   private Class<?> dWriterClass = null;
   private Class<?> dWriterImpl = null;

   private Class<?> seqHolderClass = null;
   private Class<?> dReaderClass = null;
   private Class<?> dReaderImpl = null;

   private Object seqHolder = null;
   private Object dReader = null;
   private Object dWriter = null;

   private DDS.DomainParticipant dp = null;
   private DDS.SampleInfoSeqHolder sampleInfoSeq = null;

   private final String DATA_WRITER_HELPER = "DataWriterHelper";
   private final String DATA_WRITER_IMPL = "DataWriterImpl";
   private final String WRITE_METHOD = "write";

   private final String DATA_READER_HELPER = "DataReaderHelper";
   private final String DATA_READER_IMPL = "DataReaderImpl";
   private final String NARROW_METHOD = "narrow";
   private final String READ_METHOD = "read";
   private final String TAKE_METHOD = "take";

   private final String SEQ_HOLDER = "SeqHolder";
   private final String SEQ_VALUE_FIELD = "value";
   private final String TYPE_SUPPORT = "TypeSupport";

   private final String GET_TYPE_NAME_METHOD = "get_type_name";
   private final String REGISTER_TYPE_METHOD = "register_type";
   private final String RETURN_LOAN_METHOD = "return_loan";
   private Field valueField;

   private String topicDataType;
   private boolean initialized;

   @SuppressWarnings("unused")
   private int status;

   public DDSDataAccessObj() {
   }

   /** Initialize new DDS Data Access Object */
   public Boolean initialize(String partition, String topicName,
         String topicDataType) {

      DDS.DomainParticipantFactory dpf;
      DDS.DomainParticipantQosHolder dpQos;

      DDS.Subscriber subscriber;
      DDS.SubscriberQosHolder sQos;

      DDS.Publisher publisher;
      DDS.PublisherQosHolder pQos;

      DDS.Topic topic;
      DDS.TopicQosHolder tQos;
      String topicDataTypeNS = null;

      boolean errorEncountered = false;
      this.topicDataType = topicDataType;

      dpf = DDS.DomainParticipantFactory.get_instance();
      dpQos = new DDS.DomainParticipantQosHolder();
      dpf.get_default_participant_qos(dpQos);

      tQos = new DDS.TopicQosHolder();
      sQos = new DDS.SubscriberQosHolder();

      dp = dpf.create_participant(null, dpQos.value, null,
            DDS.STATUS_MASK_NONE.value);
      dp.get_default_topic_qos(tQos);
      dp.get_default_subscriber_qos(sQos);

      tQos.value.durability.kind = DDS.DurabilityQosPolicyKind.TRANSIENT_DURABILITY_QOS;
      tQos.value.history.kind = DDS.HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;
      tQos.value.history.depth = 1;

      pQos = new DDS.PublisherQosHolder();

      sQos.value.partition.name = new String[1];
      if (partition != null)
         sQos.value.partition.name[0] = new String(partition);
      else
         sQos.value.partition.name[0] = "";

      subscriber = dp.create_subscriber(sQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      dp.get_default_publisher_qos(pQos);
      pQos.value.partition.name = new String[1];
      if (partition != null)
         pQos.value.partition.name[0] = new String(partition);
      else
         pQos.value.partition.name[0] = "";

      publisher = dp.create_publisher(pQos.value, null,
            DDS.STATUS_MASK_NONE.value);

      sampleInfoSeq = new DDS.SampleInfoSeqHolder();

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

      // Create the topic if it hasn't already been created locally
      topic = dp.find_topic(topicName, DDS.DURATION_ZERO.value);
      if (topic == null) {
         topic = dp.create_topic(topicName, topicDataTypeNS, tQos.value, null,
               DDS.STATUS_MASK_NONE.value);
      }

      // Create an untyped dataReader
      DDS.DataReader udr = subscriber.create_datareader(topic,
            DDS.DATAREADER_QOS_USE_TOPIC_QOS.value, null,
            DDS.STATUS_MASK_NONE.value);

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

      // Create an untyped dataWriter
      DDS.DataWriter udw = publisher.create_datawriter(topic,
            DDS.DATAWRITER_QOS_USE_TOPIC_QOS.value, null,
            DDS.STATUS_MASK_NONE.value);

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

      try {
         dReaderImpl = Class.forName(topicDataType + DATA_READER_IMPL);
      } catch (Exception e) {

         System.err
               .println("ERROR: Could not find data reader implementation class");
         errorEncountered = true;
      }

      try {
         seqHolderClass = Class.forName(topicDataType + SEQ_HOLDER);
         seqHolder = seqHolderClass.newInstance();
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

      return initialized;
   }

   /** DDS Data Access Object take method */
   public DDSSeqCont take() {
      return readOrTake(TAKE_METHOD);
   }

   /** DDS Data Access Object read method */
   public DDSSeqCont read() {
      return readOrTake(READ_METHOD);
   }

   @SuppressWarnings("unchecked")
   /** read or take implementation */
   private DDSSeqCont readOrTake(String method) {

      try {

         Class<?>[] methodTypeParams = { seqHolderClass,
               DDS.SampleInfoSeqHolder.class, int.class, int.class, int.class,
               int.class };
         Method readMethod = dReaderImpl.getMethod(method, methodTypeParams);
         Object[] methodParams = { seqHolder, sampleInfoSeq,
               DDS.LENGTH_UNLIMITED.value, DDS.ANY_SAMPLE_STATE.value,
               DDS.ANY_VIEW_STATE.value, DDS.ANY_INSTANCE_STATE.value };
         status = (Integer) readMethod.invoke(dReader, methodParams);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return (DDSSeqCont) seqHolder;
   }

   /** DDS Data Access Object takeInstance method */
   public DDSDataType takeInstance(String keyField, int instanceID) {
      return readOrTakeInstance(TAKE_METHOD, keyField, instanceID);
   }

   /** DDS Data Access Object readInstance method */
   public DDSDataType readInstance(String keyField, int instanceID) {
      return readOrTakeInstance(READ_METHOD, keyField, instanceID);
   }

   @SuppressWarnings("unchecked")
   /** readInstance or takeInstance implementation */
   private DDSDataType readOrTakeInstance(String method, String keyField,
         int instanceID) {

      Object msgInst = null;
      boolean found = false;

      try {

         Class<?>[] methodTypeParams = { seqHolderClass,
               DDS.SampleInfoSeqHolder.class, int.class, int.class, int.class,
               int.class };
         Method readMethod = dReaderImpl.getMethod(method, methodTypeParams);
         Object[] methodParams = { seqHolder, sampleInfoSeq,
               DDS.LENGTH_UNLIMITED.value, DDS.ANY_SAMPLE_STATE.value,
               DDS.ANY_VIEW_STATE.value, DDS.ANY_INSTANCE_STATE.value };
         status = (Integer) readMethod.invoke(dReader, methodParams);

         Object val = valueField.get(seqHolder);
         int len = Array.getLength(val);

         // Process each sample that has valid data
         for (int i = 0; i < len; ++i) {

            msgInst = Array.get(val, i);

            Class<?> _class = Class.forName(topicDataType);

            Field field = null;
            Field[] objFields = _class.getDeclaredFields();
            int value;

            // Determine if the sample matches the instanceID
            for (int n = 0; n < objFields.length; n++) {

               if (keyField.equals(objFields[n].getName())) {

                  objFields[n].setAccessible(true);
                  field = objFields[n];
                  value = (Integer) field.get(msgInst);

                  if (instanceID == value) {
                     found = true;
                     break;
                  }
               }
            }

            if (found)
               break;
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (found)
         return (DDSDataType) msgInst;
      else
         return null;
   }

   /** DDS Data Access Object writeInstance method */
   public void writeInstance(DDSDataType dataType) {

      // Invoke write method to write sample
      try {
         Class<?>[] methodTypeParams = { dataType.getClass(), long.class };
         Method writeMethod = dWriterImpl.getMethod(WRITE_METHOD,
               methodTypeParams);
         Object[] methodParams = { dataType, DDS.HANDLE_NIL.value };
         status = (Integer) writeMethod.invoke(dWriter, methodParams);
      } catch (Exception e) {
         System.err.println("ERROR: DataWriter write failed");
      }
   }

   /** DDS Data Access Object finalize method */
   public void finalize() {

      // Return loaned sequenceHolder buffer
      try {
         Class<?>[] methodTypeParams = { seqHolderClass,
               DDS.SampleInfoSeqHolder.class };
         Method returnLoan = dReaderImpl.getMethod(RETURN_LOAN_METHOD,
               methodTypeParams);
         Object[] methodParams = { seqHolder, sampleInfoSeq };
         status = (Integer) returnLoan.invoke(dReader, methodParams);
      } catch (Exception e) {
         System.err.println("ERROR: returnLoan failed");
      }
   }
}
