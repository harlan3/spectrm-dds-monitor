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

package orbisoftware.ddstools.spectrumddsmonitor.sampleviewer;

import java.net.URL;
import javax.swing.ImageIcon;

import orbisoftware.ddstools.spectrumddsmonitor.SpectrumDDSMonitor;
import orbisoftware.oricrdsm.OricSymbolMap;
import org.netbeans.swing.outline.RenderDataProvider;

class RenderSymbolMap implements RenderDataProvider {

   public RenderSymbolMap(OricSymbolMap oricSymMap) {

      this.oricSymMap = oricSymMap;

      try {

         // Set the node icons
         URL nonLeafImageURL = SpectrumDDSMonitor.class
               .getResource("icons/nonleaficon.png");

         if (nonLeafImageURL != null)
            nonLeafIcon = new ImageIcon(nonLeafImageURL);

         URL leafImageURL = SpectrumDDSMonitor.class
               .getResource("icons/leaficon.png");

         if (leafImageURL != null)
            leafIcon = new ImageIcon(leafImageURL);

      } catch (Exception e) {
      }
   }

   @Override
   public java.awt.Color getBackground(Object o) {
      return null;
   }

   @Override
   public String getDisplayName(Object o) {
      return oricSymMap.getFieldName((String) o);
   }

   @Override
   public java.awt.Color getForeground(Object o) {
      return null;
   }

   @Override
   public javax.swing.Icon getIcon(Object o) {

      if (oricSymMap.isLeaf((String) o))
         return leafIcon;
      else
         return nonLeafIcon;
   }

   @Override
   public String getTooltipText(Object o) {
      return null;
   }

   @Override
   public boolean isHtmlDisplayName(Object o) {
      return false;
   }

   private ImageIcon leafIcon = null;
   private ImageIcon nonLeafIcon = null;
   private OricSymbolMap oricSymMap;
}
