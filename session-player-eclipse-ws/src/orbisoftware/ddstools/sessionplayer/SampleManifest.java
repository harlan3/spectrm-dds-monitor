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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class SampleManifest {

   private List<SampleManifestEntry> manifestList = new ArrayList<SampleManifestEntry>();
   private long numberSamples;

   public SampleManifest() {
   }

   public void loadSessionFile(String filename) {

      DataInputStream manifestInputStream;

      /* Load the manifest */
      try {

         manifestInputStream = new DataInputStream(
               new FileInputStream(filename));
         numberSamples = manifestInputStream.readLong();

         manifestList.clear();
         for (int i = 0; i < numberSamples; i++) {

            SampleManifestEntry manifestEntry = new SampleManifestEntry(
                  manifestInputStream.readLong(),
                  manifestInputStream.readLong(),
                  manifestInputStream.readInt(), manifestInputStream.readInt());

            // Add new manifest entry
            manifestList.add(manifestEntry);
         }

         manifestInputStream.close();
      } catch (IOException E) {

         System.out.println("Exception occured when loading " + filename);
         numberSamples = 0;
         return;
      }
   }

   public SampleManifestEntry getSample(int sampleNumber) {

      return manifestList.get(sampleNumber);
   }

   public long getNumberSamples() {

      return numberSamples;
   }
}