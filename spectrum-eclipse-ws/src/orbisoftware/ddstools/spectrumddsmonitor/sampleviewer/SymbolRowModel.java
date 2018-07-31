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

import orbisoftware.oricrdsm.OricSymbolMap;
import org.netbeans.swing.outline.RowModel;

public class SymbolRowModel implements RowModel {

   @Override
   public Class<?> getColumnClass(int column) {
      switch (column) {
      case 0:
         return String.class;
      case 1:
         return String.class;
      case 2:
         return String.class;
      default:
         assert false;
      }
      return null;
   }

   @Override
   public int getColumnCount() {
      return 3;
   }

   public void setSymbolMap(OricSymbolMap oricSymbolMap) {
      this.oricSymbolMap = oricSymbolMap;
   }

   @Override
   public String getColumnName(int column) {

      switch (column) {
      case 0:
         return "Field Type";
      case 1:
         return "Data Type";
      case 2:
         return "Data Value";
      default:
         return "";
      }
   }

   @Override
   public Object getValueFor(Object node, int column) {

      switch (column) {
      case 0:
         return oricSymbolMap.getSymbolType((String) node).toString();
      case 1:
         return oricSymbolMap.getFieldType((String) node);
      case 2:
         return oricSymbolMap.getFieldValue((String) node);
      default:
         assert false;
      }
      return null;
   }

   @Override
   public boolean isCellEditable(Object node, int column) {

      switch (column) {
      case 2:
         return true;
      default:
         return false;
      }
   }

   @Override
   public void setValueFor(Object node, int column, Object value) {
      oricSymbolMap.setFieldValue((String) node, (String) value);
   }

   public OricSymbolMap getOricSymbolMap() {
      return oricSymbolMap;
   }

   private OricSymbolMap oricSymbolMap;
}
