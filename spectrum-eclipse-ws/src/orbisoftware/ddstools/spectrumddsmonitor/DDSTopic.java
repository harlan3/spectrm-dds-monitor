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

import orbisoftware.ddstools.spectrumddsmonitor.sampleviewer.SampleViewerGUI;

public class DDSTopic {

   public enum QoSProfile {
      DDS_QOS_PROFILE_1, DDS_QOS_PROFILE_2, DDS_QOS_PROFILE_3
   };

   public enum DataReaderMode {
      READ, TAKE
   };

   public enum ViewMode {
      TREE_TABLE, TEXT_LIST
   };
   
   // Topic Attributes
   public String name;
   public String partition;
   public String dataType;
   public QoSProfile topicQos;

   // DataReader mode;
   public DataReaderMode dataReaderMode;
   
   // View mode
   public ViewMode viewMode;
   
   // Auto Update
   public boolean autoUpdate;

   // Subscriber
   public boolean subscriberAttached;

   // Topic Subscriber Thread
   public boolean topicReaderWriterFinished;
   public DDSTopicReaderWriter topicReaderWriter;

   // Read Event Handler Thread
   public boolean readEventHandlerFinished;
   public DDSReadEventHandler readEventHandler;

   // Data Logger Thread
   public boolean dataLoggerFinished;
   public DDSDataLogger dataLogger;
   
   // Session Capture Thread
   public boolean sessionCaptureFinished;
   public DDSSessionCapture sessionCapture;   

   // Sample Viewer GUI
   public SampleViewerGUI sampleViewerGUI;
}
