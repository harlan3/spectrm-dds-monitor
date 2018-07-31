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

package orbisoftware.ddstools.spectrumddsmonitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import orbisoftware.ddstools.spectrumddsmonitor.sampleviewer.SampleViewerGUI;

@SuppressWarnings("serial")
class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {

   CheckBoxRenderer() {
      setHorizontalAlignment(JLabel.CENTER);
   }

   public Component getTableCellRendererComponent(JTable table, Object value,
         boolean isSelected, boolean hasFocus, int row, int column) {
      if (isSelected) {
         setForeground(table.getSelectionForeground());
         // super.setBackground(table.getSelectionBackground());
         setBackground(table.getSelectionBackground());
      } else {
         setForeground(table.getForeground());
         setBackground(table.getBackground());
      }
      setSelected((value != null && ((Boolean) value).booleanValue()));
      return this;
   }
}

class ColumnListener extends MouseAdapter {
   private JTable table;
   private static boolean allTopicsSelected = false;

   public ColumnListener(JTable t) {
      table = t;
   }

   public void mouseClicked(MouseEvent e) {

      TableColumnModel colModel = table.getColumnModel();
      int columnModelIndex = colModel.getColumnIndexAtX(e.getX());

      if (columnModelIndex == 0) {

         int rowCount = table.getModel().getRowCount();

         if (!allTopicsSelected) {
            for (int i = 0; i < rowCount; i++) {
               table.getModel().setValueAt(true, i, 0);
            }
            allTopicsSelected = true;
         } else {

            for (int i = 0; i < rowCount; i++) {
               table.getModel().setValueAt(false, i, 0);
            }
            allTopicsSelected = false;
         }
      }
   }
}

@SuppressWarnings("serial")
public class TopicSubscriberGUI implements ItemListener, ActionListener {

   private int SELECTED_COL = 0;
   private int PARTITION_COL = 1;
   private int TOPIC_COL = 2;
   private int DATATYPE_COL = 3;
   private int QOS_COL = 4;

   private JFrame frame;
   private JTable table;
   private JButton subscriberButton;
   private JButton unSubscribeButton;
   private JMenuItem openManifestMenuItem;
   private JMenuItem exitMenuItem;
   private JMenuItem readConditionTimeoutMenuItem;
   private JRadioButtonMenuItem captureOffMenuItem;
   private JRadioButtonMenuItem dataLoggerMenuItem;
   private JRadioButtonMenuItem sessionCaptureMenuItem;
   private JMenuItem helpReadmeMenuItem;
   private JMenuItem aboutMenuItem;
   private CustomFileFilter filter;
   private JFileChooser fileChooser;

   public static long sessionStartTime;

   public TopicSubscriberGUI() {

      frame = new JFrame();

      initComponents();
      frame.setVisible(true);
   }

   private void initComponents() {

      TopicManager topicManger = TopicManager.getInstance();

      frame.setTitle("Spectrum DDS Monitor");
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            shutdown();
         }
      });
      frame.setSize(500, 400);

      JScrollPane tableScrollPane;

      // Application menubar
      JMenuBar menuBar = new JMenuBar();
      JMenu fileMenu = new JMenu();
      JMenu configMenu = new JMenu();
      JMenu dataCaptureModeMenu = new JMenu();
      JMenu helpMenu = new JMenu();
      ButtonGroup buttonGroup = new ButtonGroup();

      // File Menu
      openManifestMenuItem = new JMenuItem();
      openManifestMenuItem.setText("Open URI Manifest");
      openManifestMenuItem.addActionListener(this);
      fileMenu.add(openManifestMenuItem);
      exitMenuItem = new JMenuItem();
      fileMenu.setText("File");
      exitMenuItem.setText("Exit");
      exitMenuItem.addActionListener(this);
      fileMenu.add(exitMenuItem);

      // Config Menu
      configMenu.setText("Config");
      dataCaptureModeMenu.setText("Data Capture Mode");

      buttonGroup.add(captureOffMenuItem);
      captureOffMenuItem = new JRadioButtonMenuItem();
      captureOffMenuItem.setText("Off");
      captureOffMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            TopicManager topicManger = TopicManager.getInstance();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected)
               topicManger.SetDataCaptureMode(TopicManager.DataCaptureMode.OFF);
         }
      });
      captureOffMenuItem
            .setSelected(topicManger.GetDataCaptureMode() == TopicManager.DataCaptureMode.OFF);
      buttonGroup.add(captureOffMenuItem);

      dataLoggerMenuItem = new JRadioButtonMenuItem();
      dataLoggerMenuItem.setText("Data logger");
      dataLoggerMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            TopicManager topicManger = TopicManager.getInstance();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected)
               topicManger
                     .SetDataCaptureMode(TopicManager.DataCaptureMode.DATA_LOGGER);
         }
      });
      dataLoggerMenuItem
            .setSelected(topicManger.GetDataCaptureMode() == TopicManager.DataCaptureMode.DATA_LOGGER);
      buttonGroup.add(dataLoggerMenuItem);

      sessionCaptureMenuItem = new JRadioButtonMenuItem();
      sessionCaptureMenuItem.setText("Session capture");
      sessionCaptureMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            TopicManager topicManger = TopicManager.getInstance();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected)
               topicManger
                     .SetDataCaptureMode(TopicManager.DataCaptureMode.SESSION_CAPTURE);
         }
      });
      sessionCaptureMenuItem
            .setSelected(topicManger.GetDataCaptureMode() == TopicManager.DataCaptureMode.SESSION_CAPTURE);
      buttonGroup.add(sessionCaptureMenuItem);

      dataCaptureModeMenu.add(captureOffMenuItem);
      dataCaptureModeMenu.add(dataLoggerMenuItem);
      dataCaptureModeMenu.add(sessionCaptureMenuItem);
      configMenu.add(dataCaptureModeMenu);

      readConditionTimeoutMenuItem = new JMenuItem();
      readConditionTimeoutMenuItem.setText("Set Read Condition Timeout");
      readConditionTimeoutMenuItem.addActionListener(this);
      configMenu.add(readConditionTimeoutMenuItem);

      // Help Menu
      helpMenu.setText("Help");
      helpReadmeMenuItem = new JMenuItem();
      helpReadmeMenuItem.setText("Help readme");
      helpReadmeMenuItem.addActionListener(this);
      helpMenu.add(helpReadmeMenuItem);
      aboutMenuItem = new JMenuItem();
      aboutMenuItem.setText("About");
      aboutMenuItem.addActionListener(this);
      helpMenu.add(aboutMenuItem);

      menuBar.add(fileMenu);
      menuBar.add(configMenu);
      menuBar.add(helpMenu);

      filter = new CustomFileFilter();

      // URI Manifest file chooser
      fileChooser = new JFileChooser(System.getProperty("user.dir"));

      filter.addExtension("txt");
      filter.setDescription("URI Manifest Files");

      fileChooser.setFileFilter(filter);

      populateTopicSubscriberTable();
      tableScrollPane = new JScrollPane(table);

      Container pane = frame.getContentPane();
      pane.setLayout(new GridBagLayout());

      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 1.0;
      c.weighty = 0.2;
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 0;
      c.gridy = 0;
      pane.add(menuBar, c);

      c.weightx = 1.0;
      c.weighty = 2.0;
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 0;
      c.gridy = 1;
      c.insets = new Insets(10, 10, 10, 10);
      pane.add(tableScrollPane, c);

      JPanel panel = new JPanel(new FlowLayout());

      subscriberButton = new JButton("Subscribe");
      panel.add(subscriberButton);
      subscriberButton.addActionListener(this);

      panel.add(Box.createRigidArea(new Dimension(5, 0)));

      unSubscribeButton = new JButton("UnSubscribe");
      panel.add(unSubscribeButton);
      unSubscribeButton.addActionListener(this);

      c.weightx = 1.0;
      c.weighty = 0.1;
      c.gridx = 0;
      c.gridy = 2;
      pane.add(panel, c);
   }

   private void populateTopicSubscriberTable() {

      // Topic Subscriber Table
      DefaultTableModel dm = new DefaultTableModel() {
         public boolean isCellEditable(int row, int column) {
            if ((column == 0) || (column == 1))
               return true;
            else
               return false;
         }
      };

      dm.setColumnIdentifiers(new Object[] { "Select", "Partition", "Topic",
            "Data Type", "DDS QoS Profile" });

      table = new JTable(dm);

      JCheckBox topicCheckBox = new JCheckBox();
      topicCheckBox.addItemListener(this);
      topicCheckBox.setHorizontalAlignment(JLabel.CENTER);

      CheckBoxRenderer checkBoxRenderer = new CheckBoxRenderer();
      DefaultCellEditor checkBoxEditor = new DefaultCellEditor(topicCheckBox);

      table.getColumn("Select").setCellRenderer(checkBoxRenderer);
      table.getColumn("Select").setCellEditor(checkBoxEditor);

      Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;
      Iterator<Map.Entry<String, DDSTopic>> it = topicMap.entrySet().iterator();
      int row = 0;

      while (it.hasNext()) {
         Map.Entry<String, DDSTopic> pairs = it.next();
         String key = pairs.getKey();

         dm.addRow(new Object[] { new Boolean(false),
               topicMap.get(key).partition, topicMap.get(key).name,
               topicMap.get(key).dataType,
               topicMap.get(key).topicQos.toString() });
         row++;
      }

      table = autoResizeColWidth(table, dm);
      table.getTableHeader().addMouseListener(new ColumnListener(table));
   }

   private JTable autoResizeColWidth(JTable table, DefaultTableModel model) {

      table.setModel(model);

      int margin = 5;

      for (int i = 0; i < table.getColumnCount(); i++) {

         int vColIndex = i;
         DefaultTableColumnModel colModel = (DefaultTableColumnModel) table
               .getColumnModel();
         TableColumn col = colModel.getColumn(vColIndex);
         int width = 0;

         // Get width of column header
         TableCellRenderer renderer = col.getHeaderRenderer();

         if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
         }

         Component comp = renderer.getTableCellRendererComponent(table,
               col.getHeaderValue(), false, false, 0, 0);

         width = comp.getPreferredSize().width;

         // Get maximum width of column data
         for (int r = 0; r < table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(table,
                  table.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
         }

         // Add margin
         width += 2 * margin;

         // Set the width
         col.setPreferredWidth(width);
      }

      table.setAutoCreateRowSorter(true);
      table.getRowSorter().toggleSortOrder(TOPIC_COL);

      return table;
   }

   public void itemStateChanged(ItemEvent e) {

      int row = table.getSelectionModel().getAnchorSelectionIndex();

      @SuppressWarnings("unused")
      String topicName = (String) table.getModel().getValueAt(
            table.convertRowIndexToModel(row), TOPIC_COL);

      if (e.getStateChange() == ItemEvent.SELECTED) {
      }
   }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == subscriberButton) {

         for (int i = 0; i < table.getModel().getRowCount(); i++) {

            boolean selected = (Boolean) table.getModel().getValueAt(
                  table.convertRowIndexToModel(i), SELECTED_COL);

            // Topic is selected
            if (selected) {
               String partitionName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), PARTITION_COL);
               String topicName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), TOPIC_COL);
               String keyName = "/" + partitionName + "/" + topicName;

               Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

               DDSTopic ddsTopic = topicMap.get(keyName);

               // the dds partition must have been changed by the user,
               // resulting in a new keyName which needs to be put
               // into topic manager
               if (ddsTopic == null) {
                  ddsTopic = new DDSTopic();

                  ddsTopic.partition = partitionName;
                  ddsTopic.name = topicName;
                  ddsTopic.dataType = (String) table.getModel().getValueAt(
                        table.convertRowIndexToModel(i), DATATYPE_COL);
                  ddsTopic.topicQos = DDSTopic.QoSProfile
                        .valueOf((String) table.getModel().getValueAt(
                              table.convertRowIndexToModel(i), QOS_COL));

                  TopicManager.getInstance().topicMap.put(keyName, ddsTopic);
               }

               if (!ddsTopic.subscriberAttached) {

                  // Create topic subscriber
                  ddsTopic.topicReaderWriter = new DDSTopicReaderWriter(
                        ddsTopic);
                  boolean connected = false;

                  try {
                     connected = ddsTopic.topicReaderWriter.DDSConnect(
                           ddsTopic.partition, ddsTopic.dataType);
                  } catch (Exception e2) {
                     System.out
                           .println("Exception encountered when connecting to DDS.");
                     System.out
                           .println("Please verify that ospl service is running.");
                  }
                  
                  if (connected) {
                     ddsTopic.subscriberAttached = true;

                     // Create topic GUI
                     ddsTopic.sampleViewerGUI = new SampleViewerGUI(keyName);

                     // Start subscriber thread
                     ddsTopic.topicReaderWriter.start();
                  }
               }
            }
         }
      } else if (e.getSource() == unSubscribeButton) {

         for (int i = 0; i < table.getModel().getRowCount(); i++) {

            boolean selected = (Boolean) table.getModel().getValueAt(
                  table.convertRowIndexToModel(i), SELECTED_COL);

            // Topic is selected
            if (selected) {

               String partitionName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), PARTITION_COL);
               String topicName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), TOPIC_COL);
               String keyName = "/" + partitionName + "/" + topicName;

               Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;
               DDSTopic ddsTopic = topicMap.get(keyName);

               if (ddsTopic.subscriberAttached) {
                  ddsTopic.sampleViewerGUI.shutdownReq();
               }
            }
         }
      } else if (e.getSource() == openManifestMenuItem) {

         int returnVal = fileChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION) {

            // Close all existing topics
            HashMap<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

            Iterator<Map.Entry<String, DDSTopic>> it = topicMap.entrySet()
                  .iterator();
            while (it.hasNext()) {
               Map.Entry<String, DDSTopic> pairs = it.next();
               DDSTopic topic = topicMap.get(pairs.getKey());

               if (topic.subscriberAttached)
                  topic.sampleViewerGUI.shutdownReq();
            }

            // Parse new manifest file
            SpectrumDDSMonitor.parseURIManifest(fileChooser.getSelectedFile()
                  .getPath());

            frame.getContentPane().removeAll();
            populateTopicSubscriberTable();
            initComponents();
            frame.setVisible(true);
         }

      } else if (e.getSource() == readConditionTimeoutMenuItem) {
         ReadConditionTimeoutBox readConditionTimeoutBox = new ReadConditionTimeoutBox();
         int readConditionTimeout = readConditionTimeoutBox.ShowDialog();
         TopicManager.getInstance().SetReadConditionTimeout(
               readConditionTimeout);
      } else if (e.getSource() == exitMenuItem) {
         shutdown();
      } else if (e.getSource() == helpReadmeMenuItem) {
         try {
            Runtime
                  .getRuntime()
                  .exec("firefox http://code.google.com/p/spectrum-dds-monitor/wiki/spectrum_dds_monitor");
         } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println();
         }
      } else if (e.getSource() == aboutMenuItem) {
         @SuppressWarnings("unused")
         AboutBox aboutBox = new AboutBox();
      }
   }

   public void shutdown() {

      TopicManager topicManager = TopicManager.getInstance();
      HashMap<String, DDSTopic> topicMap = topicManager.topicMap;

      Iterator<Map.Entry<String, DDSTopic>> it = topicMap.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<String, DDSTopic> pairs = it.next();
         DDSTopic topic = topicMap.get(pairs.getKey());

         if (topic.subscriberAttached)
            topic.sampleViewerGUI.shutdownReq();
      }

      // Write out the session properties file
      String loggerDir = topicManager.GetDataCaptureDirectory();
      String sessionPropertiesFile = loggerDir + "session.spf";
      try {

         long sessionEndTime = System.currentTimeMillis();

         File loggerDirHandle = new File(loggerDir);

         // if logger directory is not empty, then write session
         // properties
         if (loggerDirHandle.list().length > 0) {

            RandomAccessFile randomAccessFile = new RandomAccessFile(
                  sessionPropertiesFile, "rw");

            // Session start time
            randomAccessFile.writeLong(sessionStartTime);
            // Session end time
            randomAccessFile.writeLong(sessionEndTime);
            // Session duration
            randomAccessFile.writeLong(sessionEndTime - sessionStartTime);

            randomAccessFile.close();
         } else
            loggerDirHandle.delete();
      } catch (Exception e) {
         e.printStackTrace();
      }

      frame.dispose();
   }
}
