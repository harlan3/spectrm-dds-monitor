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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {

   CheckBoxRenderer() {
      setHorizontalAlignment(JLabel.CENTER);
   }

   public Component getTableCellRendererComponent(JTable table, Object value,
         boolean isSelected, boolean hasFocus, int row, int column) {
      if (isSelected) {
         setForeground(table.getSelectionForeground());
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
public class TopicPublisherGUI implements ItemListener, ActionListener,
      ChangeListener {

   private enum PlayerStatus {
      Stopped, Running
   }

   private int SELECTED_COL = 0;
   private int THREAD_COL = 1;
   private int TOPIC_COL = 2;
   @SuppressWarnings("unused")
   private int DATATYPE_COL = 3;
   private int LOADED_COL = 4;
   @SuppressWarnings("unused")
   private int QOS_COL = 5;

   private JFrame frame;
   private JTable table;
   private JScrollPane publisherScrollPane;
   private JMenuBar menuBar;
   private JMenuItem openManifestMenuItem;
   private JMenuItem exitMenuItem;
   private JMenuItem helpReadmeMenuItem;
   private JMenuItem aboutMenuItem;

   private JButton startThreadButton;
   private JButton stopThreadButton;
   private JButton spfSelectButton = null;
   private JButton startButton = null;
   private JButton stopButton = null;
   private JTextField spfTextField = null;
   private JLabel elapsedTimeLabel = null;
   private JLabel playerStatusLabel = null;
   private JSlider timelineSlider = null;

   private JPanel contentPanel;
   private JPanel threadPanel;
   private JPanel infoPanel;
   private JPanel statusPanel;
   private JPanel timePanel;
   private JPanel playerControlPanel;

   private GridBagLayout gridBagLayout;
   private GridBagConstraints spfSelectButtonConstraints = new GridBagConstraints();
   private GridBagConstraints menuBarConstraints = new GridBagConstraints();
   private GridBagConstraints spfTextFieldConstraints = new GridBagConstraints();
   private GridBagConstraints publisherPanelConstraints = new GridBagConstraints();
   private GridBagConstraints threadControlPanelConstraints = new GridBagConstraints();
   private GridBagConstraints infoPanelConstraints = new GridBagConstraints();
   private GridBagConstraints timelineSliderConstraints = new GridBagConstraints();
   private GridBagConstraints playerControlPanelConstraints = new GridBagConstraints();

   private JFileChooser fileChooser;
   private CustomFileFilter txtFilter;
   private CustomFileFilter spfFilter;
   private File runtimeDir;
   private String playbackDir = "logfiles";

   private Timer secondIntervalTimer;
   private PlayerStatus playerStatus;
   private boolean ignoreChangeEvent;

   private String RUNNING_TEXT = "Running";
   private String STOPPED_TEXT = "Stopped";

   public TopicPublisherGUI() {

      frame = new JFrame();

      initComponents();

      frame.setVisible(true);

      ignoreChangeEvent = false;

      runtimeDir = new File(System.getProperty("user.dir"));

      secondIntervalTimer = new javax.swing.Timer(1000, new ActionListener() {
         public void actionPerformed(ActionEvent e) {

            if (playerStatus == PlayerStatus.Running) {

               int elapsed = timelineSlider.getValue();

               if (elapsed < timelineSlider.getMaximum()) {

                  elapsed++;
                  elapsedTimeLabel.setText(Integer.toString(elapsed));
                  ignoreChangeEvent = true;
                  timelineSlider.setValue(elapsed);
               } else {

                  stopPlayer();
               }
            }
         }
      });

      secondIntervalTimer.start();
   }

   private void initComponents() {

      frame.setTitle("Spectrum DDS Session Player");
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            shutdown();
         }
      });
      frame.setSize(650, 600);

      // File chooser
      spfFilter = new CustomFileFilter();
      spfFilter.addExtension("spf");
      spfFilter.setDescription("Session Properties Files");

      txtFilter = new CustomFileFilter();
      txtFilter.addExtension("txt");
      txtFilter.setDescription("URI Manifest Files");

      fileChooser = new JFileChooser();

      contentPanel = new JPanel();
      frame.getContentPane().add(contentPanel);

      // Application menubar
      menuBar = new JMenuBar();
      JMenu fileMenu = new JMenu();
      JMenu helpMenu = new JMenu();

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

      // Menubar
      menuBar.add(fileMenu);
      menuBar.add(helpMenu);
      contentPanel.add(menuBar);

      // Session File Button
      spfSelectButton = new JButton("Session File");
      contentPanel.add(spfSelectButton);
      spfSelectButton.addActionListener(this);

      // Session file textfield
      spfTextField = new JTextField();
      contentPanel.add(spfTextField);

      // Publisher table
      populateTopicSubscriberTable();
      publisherScrollPane = new JScrollPane(table);
      contentPanel.add(publisherScrollPane);

      // Thread control
      threadPanel = new JPanel(new FlowLayout());

      // Initialize button
      startThreadButton = new JButton("Start Thread");
      startThreadButton.addActionListener(this);

      // Shutdown button
      stopThreadButton = new JButton("Stop Thread");
      stopThreadButton.addActionListener(this);

      threadPanel.add(startThreadButton);
      threadPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      threadPanel.add(stopThreadButton);

      contentPanel.add(threadPanel);

      // Info panel
      infoPanel = new JPanel();
      infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));

      // Status panel
      statusPanel = new JPanel(new FlowLayout());

      JLabel statusLabel = new JLabel("Status: ");
      statusPanel.add(statusLabel);

      playerStatusLabel = new JLabel(STOPPED_TEXT);
      statusPanel.add(playerStatusLabel);

      // Elapsed time panel
      timePanel = new JPanel(new FlowLayout());

      JLabel timeLabel = new JLabel("Elapsed: ");
      timePanel.add(timeLabel);

      elapsedTimeLabel = new JLabel("0");
      timePanel.add(elapsedTimeLabel);

      infoPanel.add(statusPanel);
      infoPanel.add(timePanel);
      contentPanel.add(infoPanel);

      // Timeline slider
      timelineSlider = new JSlider();
      timelineSlider.setValue(0);
      timelineSlider.setMaximum(0);
      timelineSlider.setPaintTicks(true);
      timelineSlider.setPaintLabels(true);
      timelineSlider.addChangeListener(this);
      contentPanel.add(timelineSlider);
      contentPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      
      // Player control panel
      playerControlPanel = new JPanel();

      // Start button
      startButton = new JButton("Start Playback");
      startButton.addActionListener(this);

      // Stop button
      stopButton = new JButton("Stop Playback");
      stopButton.addActionListener(this);

      playerControlPanel.add(startButton);
      playerControlPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      playerControlPanel.add(stopButton);

      contentPanel.add(playerControlPanel);

      applyGridbagContraints();
   }

   private void applyGridbagContraints() {

      gridBagLayout = new GridBagLayout();
      contentPanel.setLayout(gridBagLayout);

      menuBarConstraints.weightx = 1.0;
      menuBarConstraints.weighty = 1.0;
      menuBarConstraints.fill = GridBagConstraints.BOTH;
      menuBarConstraints.gridx = 0;
      menuBarConstraints.gridy = 0;
      menuBarConstraints.gridwidth = 3;

      spfSelectButtonConstraints.weightx = 1.0;
      spfSelectButtonConstraints.weighty = 1.0;
      spfSelectButtonConstraints.fill = GridBagConstraints.NONE;
      spfSelectButtonConstraints.gridx = 0;
      spfSelectButtonConstraints.gridy = 1;
      spfSelectButtonConstraints.gridwidth = 1;

      spfTextFieldConstraints.weightx = 5.0;
      spfTextFieldConstraints.weighty = 1.0;
      spfTextFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
      spfTextFieldConstraints.gridx = 1;
      spfTextFieldConstraints.gridy = 1;
      spfTextFieldConstraints.gridwidth = 2;

      publisherPanelConstraints.weightx = 1.0;
      publisherPanelConstraints.weighty = 20.0;
      publisherPanelConstraints.fill = GridBagConstraints.BOTH;
      publisherPanelConstraints.gridx = 0;
      publisherPanelConstraints.gridy = 2;
      publisherPanelConstraints.gridwidth = 3;

      timelineSliderConstraints.weightx = 1.0;
      timelineSliderConstraints.weighty = 1.0;
      timelineSliderConstraints.fill = GridBagConstraints.BOTH;
      timelineSliderConstraints.gridx = 1;
      timelineSliderConstraints.gridy = 4;
      timelineSliderConstraints.gridwidth = 2;

      threadControlPanelConstraints.weightx = 1.0;
      threadControlPanelConstraints.weighty = 1.0;
      threadControlPanelConstraints.fill = GridBagConstraints.NONE;
      threadControlPanelConstraints.gridx = 0;
      threadControlPanelConstraints.gridy = 3;
      threadControlPanelConstraints.gridwidth = 3;

      infoPanelConstraints.weightx = 1.0;
      infoPanelConstraints.weighty = 1.0;
      infoPanelConstraints.fill = GridBagConstraints.NONE;
      infoPanelConstraints.gridx = 0;
      infoPanelConstraints.gridy = 4;
      infoPanelConstraints.gridwidth = 1;

      playerControlPanelConstraints.weightx = 1.0;
      playerControlPanelConstraints.weighty = 1.0;
      playerControlPanelConstraints.fill = GridBagConstraints.NONE;
      playerControlPanelConstraints.gridx = 0;
      playerControlPanelConstraints.gridy = 5;
      playerControlPanelConstraints.gridwidth = 3;

      gridBagLayout.setConstraints(menuBar, menuBarConstraints);
      gridBagLayout.setConstraints(spfSelectButton, spfSelectButtonConstraints);
      gridBagLayout.setConstraints(spfTextField, spfTextFieldConstraints);
      gridBagLayout.setConstraints(publisherScrollPane,
            publisherPanelConstraints);
      gridBagLayout.setConstraints(threadPanel, threadControlPanelConstraints);
      gridBagLayout.setConstraints(infoPanel, infoPanelConstraints);
      gridBagLayout.setConstraints(timelineSlider, timelineSliderConstraints);
      gridBagLayout.setConstraints(playerControlPanel,
            playerControlPanelConstraints);

   }

   private void populateTopicSubscriberTable() {

      // Topic Subscriber Table
      DefaultTableModel dm = new DefaultTableModel() {
         public boolean isCellEditable(int row, int column) {
            if (column == 0)
               return true;
            else
               return false;
         }
      };

      dm.setColumnIdentifiers(new Object[] { "Select", "Thread", "Topic",
            "Data Type", "Loaded", "DDS QoS Profile" });

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

         dm.addRow(new Object[] { new Boolean(false), "Halted",
               topicMap.get(key).name, topicMap.get(key).dataType, "No",
               topicMap.get(key).topicQos.toString() });
         row++;
      }

      table = autoResizeColWidth(table, dm);
      table.getTableHeader().addMouseListener(new ColumnListener(table));
   }

   private void updateLoadedData(String sessionDirectory) {

      TableModel tableModel = table.getModel();

      for (int i = 0; i < table.getRowCount(); i++) {

         String topicText = (String) tableModel.getValueAt(i, TOPIC_COL);

         File topicJSON = new File(sessionDirectory + File.separator
               + topicText + ".json");

         if (topicJSON.exists()) {
            tableModel.setValueAt("Yes", i, LOADED_COL);
            tableModel.setValueAt(true, i, SELECTED_COL);
         } else {
            tableModel.setValueAt("No", i, LOADED_COL);
            tableModel.setValueAt(false, i, SELECTED_COL);
         }
      }
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

   public void startPlayer() {

      PlaybackEvent playbackEvent = new PlaybackEvent(this,
            PlaybackEvent.PlaybackEventType.StartEvent,
            SessionPropertiesFile.getInstance().sessionStartTime,
            System.currentTimeMillis(), timelineSlider.getValue() * 1000);

      playerStatus = PlayerStatus.Running;
      playerStatusLabel.setText(RUNNING_TEXT);

      PlaybackEventManager.getInstance().notifyPlaybackEvent(playbackEvent);
   }

   public void stopPlayer() {

      PlaybackEvent playbackEvent = new PlaybackEvent(this,
            PlaybackEvent.PlaybackEventType.StopEvent, 0, 0, 0);

      playerStatus = PlayerStatus.Stopped;
      playerStatusLabel.setText(STOPPED_TEXT);

      PlaybackEventManager.getInstance().notifyPlaybackEvent(playbackEvent);
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

      if (e.getSource() == startThreadButton) {

         for (int i = 0; i < table.getModel().getRowCount(); i++) {

            boolean selected = (Boolean) table.getModel().getValueAt(
                  table.convertRowIndexToModel(i), SELECTED_COL);

            // Topic is selected
            if (selected) {

               String topicName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), TOPIC_COL);
               String keyName = topicName;

               Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

               DDSTopic ddsTopic = topicMap.get(keyName);

               if (!ddsTopic.publisherAttached) {

                  // Create topic publisher
                  ddsTopic.topicSampleWriter = new DDSTopicSampleWriter(
                        ddsTopic);

                  boolean connected = false;

                  try {
                     connected = ddsTopic.topicSampleWriter.DDSConnect(
                           ddsTopic.partition, ddsTopic.dataType);
                  } catch (Exception e2) {
                     System.out
                           .println("Exception encountered when connecting to DDS.");
                     System.out
                           .println("Please verify that ospl service is running.");
                  }

                  if (connected) {
                     ddsTopic.publisherAttached = true;

                     table.getModel().setValueAt(RUNNING_TEXT,
                           table.convertRowIndexToModel(i), THREAD_COL);
                     // Start subscriber thread
                     ddsTopic.topicSampleWriter.start();
                  }
               }
            }
         }
      } else if (e.getSource() == stopThreadButton) {

         for (int i = 0; i < table.getModel().getRowCount(); i++) {

            boolean selected = (Boolean) table.getModel().getValueAt(
                  table.convertRowIndexToModel(i), SELECTED_COL);

            // Topic is selected
            if (selected) {

               String topicName = (String) table.getModel().getValueAt(
                     table.convertRowIndexToModel(i), TOPIC_COL);
               String keyName = topicName;

               Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;
               DDSTopic ddsTopic = topicMap.get(keyName);

               table.getModel().setValueAt("Halted",
                     table.convertRowIndexToModel(i), 1);
               if (ddsTopic.publisherAttached) {
                  ddsTopic.topicSampleWriter.shutdownReq();
               }
            }
         }
      } else if (e.getSource() == startButton) {

         if (timelineSlider.getValue() < timelineSlider.getMaximum()) {

            startPlayer();
         }
      } else if (e.getSource() == stopButton) {

         stopPlayer();

      } else if (e.getSource() == openManifestMenuItem) {

         int returnVal;

         stopPlayer();

         fileChooser.setCurrentDirectory(runtimeDir);
         fileChooser.setFileFilter(txtFilter);

         returnVal = fileChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION) {

            // Close all existing topics
            HashMap<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

            Iterator<Map.Entry<String, DDSTopic>> it = topicMap.entrySet()
                  .iterator();
            while (it.hasNext()) {
               Map.Entry<String, DDSTopic> pairs = it.next();
               DDSTopic topic = topicMap.get(pairs.getKey());

               if (topic.publisherAttached)
                  topic.topicSampleWriter.shutdownReq();
            }

            frame.getContentPane().removeAll();
            populateTopicSubscriberTable();
            initComponents();
            frame.setVisible(true);
         }
      } else if (e.getSource() == spfSelectButton) {

         int returnVal;

         stopPlayer();

         File dir = new File(runtimeDir + File.separator + playbackDir);

         fileChooser.setCurrentDirectory(dir);
         fileChooser.setFileFilter(spfFilter);

         returnVal = fileChooser.showOpenDialog(null);

         if (returnVal == JFileChooser.APPROVE_OPTION) {

            SessionPropertiesFile sessionPropertiesFile = SessionPropertiesFile
                  .getInstance();

            spfTextField.setText(fileChooser.getSelectedFile()
                  .getAbsolutePath());

            sessionPropertiesFile.loadSessionPlaybackFile(fileChooser
                  .getSelectedFile());

            updateLoadedData(fileChooser.getSelectedFile().getParent());

            final int tickSpacing = (int) sessionPropertiesFile.sessionDuration / 1000 / 4;
            int maximum = (int) sessionPropertiesFile.sessionDuration / 1000;

            timelineSlider.setValue(0);
            timelineSlider.setMaximum(maximum);

            // After all pending events have been proccessed,
            // create new timeline ticks.
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

               public void run() {

                  try {
                     Thread.sleep(100);
                  } catch (InterruptedException e) {
                  }
                  timelineSlider.setLabelTable(null);
                  timelineSlider.setMajorTickSpacing(tickSpacing);
               }
            });
         }
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

   public void stateChanged(ChangeEvent ce) {

      if (ce.getSource() == timelineSlider) {

         if (ignoreChangeEvent)
            elapsedTimeLabel
                  .setText(Integer.toString(timelineSlider.getValue()));
         else {
            elapsedTimeLabel
                  .setText(Integer.toString(timelineSlider.getValue()));

            if (playerStatus != PlayerStatus.Stopped) {

               stopPlayer();
            }
         }

         ignoreChangeEvent = false;
      }
   }

   public void shutdown() {

      HashMap<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

      Iterator<Map.Entry<String, DDSTopic>> it = topicMap.entrySet().iterator();

      while (it.hasNext()) {

         Map.Entry<String, DDSTopic> pairs = it.next();
         DDSTopic topic = topicMap.get(pairs.getKey());

         if (topic.publisherAttached)
            topic.topicSampleWriter.shutdownReq();
      }

      secondIntervalTimer.stop();

      frame.dispose();
   }
}
