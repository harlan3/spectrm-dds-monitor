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

import java.util.Vector;

public class PlaybackEventManager {

   private Vector<PlaybackEventListener> eventListeners = new Vector<PlaybackEventListener>();

   private static PlaybackEventManager instance = null;

   protected PlaybackEventManager() {
   }

   public static PlaybackEventManager getInstance() {

      if (instance == null) {
         instance = new PlaybackEventManager();
      }
      return instance;
   }

   public synchronized void addPlaybackEventListener(
         PlaybackEventListener playbackEventListener) {
      eventListeners.addElement(playbackEventListener);
   }

   public synchronized void removePlaybackEventListener(
         PlaybackEventListener hce) {
      eventListeners.removeElement(hce);
   }

   public void notifyPlaybackEvent(PlaybackEvent newPlaybackEvent) {

      for (PlaybackEventListener eventListener : eventListeners) {
         eventListener.newPlaybackEvent(newPlaybackEvent);
      }
   }
}
