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

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class AboutBox extends javax.swing.JDialog implements ActionListener {

   private String softwareVersion = new String("0.8.8.3");
   private JButton okButton;
   private JButton appHomePageButton;

   public AboutBox() {
      initComponents();
   }

   private void initComponents() {

      JLabel appTitleLabel = new javax.swing.JLabel();
      JLabel versionLabel = new javax.swing.JLabel();
      JLabel vendorLabel = new javax.swing.JLabel();
      JLabel copyRightLabel = new javax.swing.JLabel();
      JLabel appDescLabel = new javax.swing.JLabel();

      Container pane = getContentPane();
      pane.setLayout(new GridBagLayout());

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setTitle("About");
      setModal(true);
      setResizable(false);

      appTitleLabel.setFont(appTitleLabel.getFont().deriveFont(
            appTitleLabel.getFont().getStyle() | java.awt.Font.BOLD,
            appTitleLabel.getFont().getSize() + 4));
      appTitleLabel.setText("Spectrum DDS Session Player");

      appDescLabel.setText("DDS session playback tool");

      versionLabel.setText("Version " + softwareVersion);

      vendorLabel.setText("Orbis Software");
      copyRightLabel.setText("Copyright Harlan Murphy");

      appHomePageButton = new javax.swing.JButton();
      appHomePageButton.setFocusPainted(false);
      appHomePageButton.setHorizontalAlignment(SwingConstants.LEFT);
      appHomePageButton.setContentAreaFilled(false);
      appHomePageButton.setBorderPainted(false);
      appHomePageButton.setOpaque(false);
      appHomePageButton.setForeground(Color.BLUE.darker());
      appHomePageButton
            .setText("<html><u>http://code.google.com/p/spectrum-dds-monitor</u></html>");
      appHomePageButton.addActionListener(this);

      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 1.0;
      c.weighty = 0.05;
      c.fill = GridBagConstraints.BOTH;

      c.gridx = 0;
      c.gridy = 0;
      c.insets = new Insets(10, 20, 0, 0);
      pane.add(appTitleLabel, c);

      c.gridx = 0;
      c.gridy = 1;
      c.insets = new Insets(0, 20, 0, 0);
      pane.add(appDescLabel, c);

      c.gridx = 0;
      c.gridy = 2;
      c.weighty = 0.5;
      pane.add(versionLabel, c);

      c.gridx = 0;
      c.gridy = 3;
      c.weighty = 0.05;
      pane.add(vendorLabel, c);

      c.gridx = 0;
      c.gridy = 4;
      pane.add(copyRightLabel, c);

      c.gridx = 0;
      c.gridy = 5;
      c.weighty = 0.5;
      c.insets = new Insets(0, 5, 0, 0);
      pane.add(appHomePageButton, c);

      JPanel panel = new JPanel(new FlowLayout());

      okButton = new JButton("Ok");
      panel.add(okButton);
      okButton.addActionListener(this);

      c.gridx = 0;
      c.gridy = 6;
      pane.add(panel, c);

      setSize(400, 220);
      setVisible(true);
   }

   public void actionPerformed(ActionEvent e) {

      if (e.getSource() == okButton)
         this.dispose();
      else if (e.getSource() == appHomePageButton) {
         try {
            Runtime.getRuntime().exec(
                  "firefox http://code.google.com/p/spectrum-dds-monitor/");
         } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println();
         }
      }
   }
}
