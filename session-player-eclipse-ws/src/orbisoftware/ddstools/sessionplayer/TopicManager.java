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

import java.util.HashMap;

public class TopicManager {

   public enum LoggerMode {
      DISABLED, ENABLED
   };

   public HashMap<String, DDSTopic> topicMap = new HashMap<String, DDSTopic>();

   private static TopicManager instance = null;

   private LoggerMode loggerMode = LoggerMode.DISABLED;
   private int readConditionTimeout = 250;
   private String loggerDir = null;

   protected TopicManager() {

      topicMap.clear();
   }

   public static TopicManager getInstance() {

      if (instance == null) {
         instance = new TopicManager();
      }
      return instance;
   }

   public void SetReadConditionTimeout(int readConditionTimeout) {
      this.readConditionTimeout = readConditionTimeout;
   }

   public int GetReadConditionTimeout() {
      return readConditionTimeout;
   }

   public void SetLoggerMode(LoggerMode loggerMode) {
      this.loggerMode = loggerMode;
   }

   public LoggerMode GetLoggerMode() {
      return loggerMode;
   }

   public void SetLoggerDirectory(String loggerDir) {
      this.loggerDir = loggerDir;
   }

   public String GetLoggerDir() {
      return loggerDir;
   }
}
