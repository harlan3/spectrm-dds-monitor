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

import java.util.EventObject;

@SuppressWarnings("serial")
public class PlaybackEvent extends EventObject {
   public enum PlaybackEventType {
      StartEvent, StopEvent
   }

   private PlaybackEventType eventType;
   private long sessionStartTime;
   private long commandedStartTime;
   private long elapsedTime;

   public PlaybackEvent(Object source, PlaybackEventType eventType,
         long sessionStartTime, long commandedStartTime, long elapsedTime) {
      super(source);

      this.eventType = eventType;
      this.sessionStartTime = sessionStartTime;
      this.commandedStartTime = commandedStartTime;
      this.elapsedTime = elapsedTime;
   }

   public PlaybackEventType getPlaybackEventType() {

      return eventType;
   }

   public long getSessionStartTime() {

      return sessionStartTime;
   }

   public long getCommandedStartTime() {

      return commandedStartTime;
   }

   public long getElapsedTime() {

      return elapsedTime;
   }
}
