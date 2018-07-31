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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import orbisoftware.ddstools.sessionplayer.DDSTopic;
import orbisoftware.ddstools.sessionplayer.TopicManager;

public class SessionPlayer {

   public static void main(String[] args) {

      javax.swing.SwingUtilities.invokeLater(new Runnable() {

         public void run() {

            File dir = new File(".");
            String fileName = null;

            try {
               // Default URI manifest
               fileName = dir.getCanonicalPath() + File.separator
                     + "URI_Manifest.txt";
            } catch (IOException e) {
            }

            parseURIManifest(fileName);

            @SuppressWarnings("unused")
            TopicPublisherGUI topicView = new TopicPublisherGUI();
         }
      });
   }

   public static void parseURIManifest(String fileName) {

      TopicManager topicManager = TopicManager.getInstance();
      topicManager.topicMap.clear();

      try {

         BufferedReader buffReader = new BufferedReader(
               new FileReader(fileName));
         String uriLine;

         while ((uriLine = buffReader.readLine()) != null) {

            DDSTopic ddsTopic = new DDSTopic();
            String topicKey = new String();

            boolean uriValid = false;

            if (uriLine.trim().charAt(0) != '#') {

               StringTokenizer uriLineComponents = new StringTokenizer(uriLine,
                     ",");

               String uriSection = uriLineComponents.nextToken().trim();
               String qosSetting = uriLineComponents.nextToken().trim();

               StringTokenizer uriParts = new StringTokenizer(uriSection, "/");

               if (uriParts.countTokens() == 3) {

                  // ddsTopic.partition = uriParts.nextToken();
                  uriParts.nextToken();
                  ddsTopic.partition = "*";
                  ddsTopic.name = uriParts.nextToken();
                  ddsTopic.dataType = uriParts.nextToken();
                  topicKey = ddsTopic.name;

                  uriValid = true;
               }

               if (qosSetting == null) {
                  qosSetting = new String("DDS_QOS_PROFILE_1");
               }

               if (uriValid) {

                  try {
                     ddsTopic.topicQos = DDSTopic.QoSProfile
                           .valueOf(qosSetting);
                  } catch (Exception e) {
                     System.out
                           .println("Unsupport QoS profile referenced in URI manifest.");
                     System.exit(1);
                  }
                  topicManager.topicMap.put(topicKey, ddsTopic);
               }
            }
         }
         buffReader.close();
      } catch (IOException e) {
         System.err.println("Error: Could not read " + fileName);
      }
   }
}
