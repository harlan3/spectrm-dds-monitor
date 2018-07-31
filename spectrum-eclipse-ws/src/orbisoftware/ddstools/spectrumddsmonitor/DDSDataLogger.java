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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ecollege.gson.GsonExt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DDSDataLogger extends Thread implements PropertyChangeListener {

   private BlockingQueue<DDSSamples> queue = null;
   private List<DDSSamples> logEventList = null;
   private DDSTopic ddsTopic;
   private String dataLoggerFileName = null;
   private FileWriter fileWriter;
   private BufferedWriter dataLoggerWriter;

   public DDSDataLogger(DDSTopic ddsTopic) {

      try {
         TopicManager topicManager = TopicManager.getInstance();
         dataLoggerFileName = new String(topicManager.GetDataCaptureDirectory()
               + ddsTopic.name + ".xml");
         fileWriter = new FileWriter(dataLoggerFileName);
         dataLoggerWriter = new BufferedWriter(fileWriter);
         dataLoggerWriter.write("<root>");
      } catch (Exception e) {// Catch exception if any
         System.err.println("ERROR: " + e.getMessage());
      }

      queue = new LinkedBlockingQueue<DDSSamples>();
      this.ddsTopic = ddsTopic;
      ddsTopic.dataLoggerFinished = false;
   }

   public void propertyChange(PropertyChangeEvent evt) {

      if (evt.getPropertyName().toString().equals("ddsDataLoggerEvent")) {

         synchronized (queue) {
            queue.add((DDSSamples) evt.getNewValue());
            queue.notify();
         }
      }
   }

   public void run() {

      while (!ddsTopic.dataLoggerFinished) {

         logEventList = new LinkedList<DDSSamples>();

         try {
            // block until notified that more logEvents have been received
            synchronized (queue) {
               queue.wait();
               queue.drainTo(logEventList);
            }
         } catch (InterruptedException e) {
            shutdown();
         }

         for (DDSSamples ddsSample : logEventList) {

            try {

               Gson gson = new GsonBuilder().setPrettyPrinting()
                     .serializeNulls().serializeSpecialFloatingPointValues()
                     .create();
               String xml = GsonExt.toXml(gson.toJson(ddsSample));
               dataLoggerWriter.write(xml);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   public void shutdownReq() {
      this.interrupt();
   }

   private void shutdown() {
      queue.clear();
      logEventList.clear();
      try {
         dataLoggerWriter.write("</root>");
         dataLoggerWriter.close();

         File tmpLoggerFile = new File(dataLoggerFileName);

         // Remove file if 0 length or if it only contains root element
         if ((tmpLoggerFile.length() == 0) || (tmpLoggerFile.length() == 13))
            tmpLoggerFile.delete();

      } catch (IOException e) {
      }
      ddsTopic.dataLoggerFinished = true;
   }
}
