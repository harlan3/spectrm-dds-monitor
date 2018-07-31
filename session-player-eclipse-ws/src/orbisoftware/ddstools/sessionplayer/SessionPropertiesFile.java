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
import java.io.RandomAccessFile;

public class SessionPropertiesFile {

   private static SessionPropertiesFile instance = null;

   public String sessionPropertiesFile;
   public String sessionPropertiesDirectory;

   public long sessionStartTime;
   public long sessionEndTime;
   public long sessionDuration;

   protected SessionPropertiesFile() {

   };

   public static SessionPropertiesFile getInstance() {

      if (instance == null) {
         instance = new SessionPropertiesFile();
      }
      return instance;
   }

   public void loadSessionPlaybackFile(File _sessionPropertiesFile) {

      RandomAccessFile randomAccessFile;

      try {

         sessionPropertiesFile = _sessionPropertiesFile.getPath();
         sessionPropertiesDirectory = _sessionPropertiesFile.getParent();

         randomAccessFile = new RandomAccessFile(sessionPropertiesFile, "rw");

         // Session start time
         sessionStartTime = randomAccessFile.readLong();

         // Session end time
         sessionEndTime = randomAccessFile.readLong();

         // Session duration
         sessionDuration = randomAccessFile.readLong();

         randomAccessFile.close();

      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
