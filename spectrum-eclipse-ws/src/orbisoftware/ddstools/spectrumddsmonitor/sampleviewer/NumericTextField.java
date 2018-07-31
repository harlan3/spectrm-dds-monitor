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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class NumericTextField extends JTextField {
   public NumericTextField(String _initialStr, int _col) {
      super(_initialStr, _col);

      this.addKeyListener(new KeyAdapter() {
         public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();

            if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                  || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB) || (Character
                  .isDigit(c)))) {
               e.consume();
            }
         }
      });
   }

   public NumericTextField(int _col) {
      this("", _col);
   }
}