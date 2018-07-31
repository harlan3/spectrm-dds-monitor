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

import javax.swing.tree.TreeModel;

import orbisoftware.oricrdsm.OricSymbolMap;

public class SymbolMapTreeModel implements TreeModel {

   private String rootSymbol;
   private OricSymbolMap oricSymMap;

   public SymbolMapTreeModel(OricSymbolMap oricSymMap) {
      this.oricSymMap = oricSymMap;
      this.rootSymbol = oricSymMap.getBaseSymbol();
   }

   @Override
   public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
   }

   @Override
   public Object getChild(Object parent, int index) {
      return oricSymMap.getChildByIndex((String) parent, index);
   }

   @Override
   public int getChildCount(Object parent) {
      return oricSymMap.getChildCount((String) parent);
   }

   @Override
   public int getIndexOfChild(Object parent, Object child) {
      return oricSymMap.getIndexOfChild((String) parent, (String) child);
   }

   @Override
   public Object getRoot() {
      return rootSymbol;
   }

   @Override
   public boolean isLeaf(Object symbol) {
      return oricSymMap.isLeaf((String) symbol);
   }

   @Override
   public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
   }

   @Override
   public void valueForPathChanged(javax.swing.tree.TreePath path,
         Object newValue) {
   }
}
