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
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ecollege.gson.GsonExt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import orbisoftware.ddstools.spectrumddsmonitor.DDSTopic.ViewMode;
import orbisoftware.oricrdsm.OricSymbolMap;

public class DDSReadEventHandler extends Thread implements
      PropertyChangeListener {

   private BlockingQueue<DDSSamples> queue = null;
   private List<DDSSamples> readEventList = null;
   private DDSTopic ddsTopic;
   private String OBJ_TO_SYM_MAP_METHOD = "objToSymMap";
   private Class<?> oricSymbolMapClass = null;

   public DDSReadEventHandler(DDSTopic ddsTopic) {

      queue = new LinkedBlockingQueue<DDSSamples>();
      this.ddsTopic = ddsTopic;
      ddsTopic.readEventHandlerFinished = false;
   }

   public void propertyChange(PropertyChangeEvent evt) {

      if (evt.getPropertyName().toString().equals("ddsReadEvent")) {

         synchronized (queue) {
            queue.add((DDSSamples) evt.getNewValue());
            queue.notify();
         }
      }
   }

   public void run() {

      Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .serializeSpecialFloatingPointValues().create();

      while (!ddsTopic.readEventHandlerFinished) {

         readEventList = new LinkedList<DDSSamples>();

         try {
            // block until notified that more readEvents have been received
            synchronized (queue) {
               queue.wait();
               queue.drainTo(readEventList);
            }
         } catch (InterruptedException e) {
            shutdown();
         }

         if (readEventList.size() > 0) {

            try {

               OricSymbolMap oricSymMap = new OricSymbolMap();
               DDSSamples readEvent = readEventList
                     .get(readEventList.size() - 1);

               oricSymbolMapClass = oricSymMap.getClass();
               Class<?>[] methodTypeParams = { Object.class };
               Method objToSymMap = oricSymbolMapClass.getMethod(
                     OBJ_TO_SYM_MAP_METHOD, methodTypeParams);
               Object[] methodParams = { readEvent.seqHolder };
               objToSymMap.invoke(oricSymMap, methodParams);

               if (ddsTopic.viewMode == ViewMode.TREE_TABLE) {

                  ddsTopic.sampleViewerGUI.updateTreeTable(oricSymMap);
               } else if (ddsTopic.viewMode == ViewMode.TEXT_LIST) {

                  String xml = GsonExt.toXml(gson.toJson(readEvent.seqHolder));
                  ddsTopic.sampleViewerGUI.updateTextTable(xml);
               }
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
      readEventList.clear();
      ddsTopic.readEventHandlerFinished = true;
   }
}
