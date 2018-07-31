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

import java.awt.Container;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ResizeBox extends javax.swing.JDialog implements ActionListener {

   private JButton okButton;
   private NumericTextField arraySizeTextField;
   private int result;

   public int ShowDialog() {
      initComponents();
      setVisible(true);
      return result;
   }

   private void initComponents() {

      JLabel newSizeLabel = new javax.swing.JLabel();

      Container pane = getContentPane();
      pane.setLayout(new GridBagLayout());

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setTitle("Resize Array");
      setModal(true);
      setResizable(false);

      newSizeLabel.setText("New size of array: ");
      arraySizeTextField = new NumericTextField("1", 3);

      JPanel panel1 = new JPanel(new FlowLayout());

      panel1.add(newSizeLabel);
      panel1.add(arraySizeTextField);

      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 1.0;
      c.weighty = 1.0;
      c.fill = GridBagConstraints.BOTH;

      c.gridx = 0;
      c.gridy = 0;
      c.insets = new Insets(10, 0, 0, 0);
      pane.add(panel1, c);

      JPanel panel2 = new JPanel(new FlowLayout());

      okButton = new JButton("Ok");
      panel2.add(okButton);
      okButton.addActionListener(this);

      c.gridx = 0;
      c.gridy = 2;
      pane.add(panel2, c);

      setSize(300, 120);
   }

   @SuppressWarnings("deprecation")
   public boolean handleEvent(Event event) {
      if (event.target == arraySizeTextField && event.id == Event.KEY_PRESS) {
         char c = (char) event.key;
         if (c >= '0' && c <= '9') {
            // keep digit
            return super.handleEvent(event);
         } else if (Character.isISOControl(c)) {
            // keep control character (like del, bksp)
            return super.handleEvent(event);
         } else {
            // discard Character
            return true;
         }
      }
      return super.handleEvent(event);
   }

   public void actionPerformed(ActionEvent e) {

      if (Integer.parseInt(arraySizeTextField.getText()) > 0) {
         result = Integer.parseInt(arraySizeTextField.getText());
         this.dispose();
      } else
         arraySizeTextField.setText("1");
   }
}
