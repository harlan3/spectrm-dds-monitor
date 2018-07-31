/*
 *  Spectrum DDS data monitor tool
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

package orbisoftware.ddstools.spectrumddsmonitor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DDSSessionCapture extends Thread implements PropertyChangeListener {

   private BlockingQueue<DDSSamples> queue = null;
   private List<DDSSamples> logEventList = null;
   private DDSTopic ddsTopic;

   private String manifestFilename = null;
   private DataOutputStream manifestOutputStream;

   private String jsonFilename = null;
   private DataOutputStream jsonOutputStream;

   private long prevSampleTime;
   private long numberSamplesCount;

   public DDSSessionCapture(DDSTopic ddsTopic) {

      try {

         TopicManager topicManager = TopicManager.getInstance();

         manifestFilename = new String(topicManager.GetDataCaptureDirectory()
               + ddsTopic.name + ".man");
         File manifestFile = new File(manifestFilename);
         manifestOutputStream = new DataOutputStream(new BufferedOutputStream(
               new FileOutputStream(manifestFile)));

         jsonFilename = new String(topicManager.GetDataCaptureDirectory()
               + ddsTopic.name + ".json");
         File jsonFile = new File(jsonFilename);
         jsonOutputStream = new DataOutputStream(new BufferedOutputStream(
               new FileOutputStream(jsonFile)));

      } catch (Exception e) {
         System.err.println("ERROR: " + e.getMessage());
      }

      queue = new LinkedBlockingQueue<DDSSamples>();
      this.ddsTopic = ddsTopic;
      ddsTopic.sessionCaptureFinished = false;
      numberSamplesCount = 0;
   }

   public void propertyChange(PropertyChangeEvent evt) {

      if (evt.getPropertyName().toString().equals("ddsSessionCaptureEvent")) {

         synchronized (queue) {
            queue.add((DDSSamples) evt.getNewValue());
            queue.notify();
         }
      }
   }

   public void run() {

      Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .serializeSpecialFloatingPointValues().create();
      
      SampleManifestEntry sampleManifestEntry = new SampleManifestEntry(0L, 0,
            0, 0);
      String jsonString;

      // Initialize to session start time
      prevSampleTime = TopicSubscriberGUI.sessionStartTime;
      
      // Initialize sample file position
      sampleManifestEntry.sampleFilePos = 0;
      
      // Write out a place holder for the number samples count
      try {
         manifestOutputStream.writeLong(0L);
      } catch (IOException e1) { }
      
      while (!ddsTopic.sessionCaptureFinished) {

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
               
               // Convert sample to json and write to output stream
               jsonString = gson.toJson(ddsSample.seqHolder);
               jsonOutputStream.writeBytes(jsonString);
               
               sampleManifestEntry.sampleTimeStamp = 
                  System.currentTimeMillis();
               sampleManifestEntry.sampleSize = jsonString.length();
               sampleManifestEntry.sampleTimeDelta = 
                  (int) (sampleManifestEntry.sampleTimeStamp - prevSampleTime);
               
               // Write manifest entry to output stream
               manifestOutputStream.writeLong(sampleManifestEntry.sampleTimeStamp);
               manifestOutputStream.writeLong(sampleManifestEntry.sampleFilePos);
               manifestOutputStream.writeInt(sampleManifestEntry.sampleSize);
               manifestOutputStream.writeInt(sampleManifestEntry.sampleTimeDelta);
               
               // Update prevSampleTime
               prevSampleTime = sampleManifestEntry.sampleTimeStamp;
               
               // Update sampleFilePosition for next sample
               sampleManifestEntry.sampleFilePos = jsonOutputStream.size();
               
               // Update number samples count
               numberSamplesCount++;
            } catch (Exception e) {
               System.err.println("ERROR: could not write session samples");
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
         
         manifestOutputStream.close();
         jsonOutputStream.close();

         // Update the number samples count
         RandomAccessFile randomAccessFile = new RandomAccessFile(manifestFilename, "rw");         
         randomAccessFile.seek(0);
         randomAccessFile.writeLong(numberSamplesCount);
         randomAccessFile.close();
         
         File tmpManifestFile = new File(jsonFilename);
         File tmpJsonFile = new File(manifestFilename);

         if (tmpManifestFile.length() == 0) {
            tmpManifestFile.delete();
            tmpJsonFile.delete();
         }
         
      } catch (IOException e) {
         e.printStackTrace();
      }
      ddsTopic.sessionCaptureFinished = true;
   }
}
