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

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.beans.PropertyChangeSupport;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

import orbisoftware.ddstools.spectrumddsmonitor.DDSTopic;
import orbisoftware.ddstools.spectrumddsmonitor.TopicManager;
import orbisoftware.oricrdsm.OricSymbolMap;

import org.netbeans.swing.outline.DefaultOutlineModel;

public class SampleViewerGUI implements ActionListener, ItemListener {

   private int oricSymMapCount;
   private org.netbeans.swing.outline.Outline outline;
   private SampleTextList sampleTextList;
   private SymbolRowModel symbolRowModel;
   private JFrame frame;
   private boolean treeTableInitialized;
   private JButton publishButton;
   private JButton injectButton;
   private JButton resizeButton;
   private JButton clearTextListButton;
   private JCheckBox autoUpdateCheckBox;
   private DDSTopic ddsTopic;
   private String resizeNodeSymbol;

   private JRadioButtonMenuItem readerReadMenuItem;
   private JRadioButtonMenuItem readerTakeMenuItem;
   private JRadioButtonMenuItem treeMenuItem;
   private JRadioButtonMenuItem listMenuItem;

   private ListSelectionListener listSelectionListener = null;

   private JPanel treeTablePanel;

   private JPanel cardSampleViewerPanel;

   private PropertyChangeSupport propertyChangeSupport;

   public SampleViewerGUI(String keyName) {

      Map<String, DDSTopic> topicMap = TopicManager.getInstance().topicMap;

      ddsTopic = topicMap.get(keyName);

      // Create JFrame
      frame = new JFrame();
      frame.setBounds(20, 20, 700, 400);
      frame.setTitle(keyName);
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            shutdownReq();
         }
      });

      initSampleViewerGUI();
   }

   public void initSampleViewerGUI() {

      Container pane = frame.getContentPane();
      pane.setLayout(new GridBagLayout());

      JPanel mainControlPanel = new JPanel();
      mainControlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      JPanel dataReaderPanel = new JPanel();
      JPanel viewModePanel = new JPanel();
      JPanel sampleControlPanel = new JPanel();

      // Sample view with outline tree table
      treeTablePanel = new JPanel();
      treeTablePanel.setLayout(new BorderLayout());
      treeTablePanel.add(new JScrollPane(outline));

      // Sample view with text list
      JPanel textListPanel = new JPanel();
      textListPanel.setLayout(new BorderLayout());
      sampleTextList = new SampleTextList();
      textListPanel.add(new JScrollPane(sampleTextList));

      cardSampleViewerPanel = new JPanel(new CardLayout());
      cardSampleViewerPanel.add(treeTablePanel, "TREE_TABLE");
      cardSampleViewerPanel.add(textListPanel, "TEXT_LIST");

      createCommonControlPanels(dataReaderPanel, viewModePanel,
            sampleControlPanel);

      mainControlPanel.add(dataReaderPanel);
      mainControlPanel.add(viewModePanel);
      mainControlPanel.add(sampleControlPanel);

      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 1.0;
      c.weighty = 1;
      c.gridx = 0;
      c.gridy = 0;
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(10, 0, 0, 15);
      pane.add(mainControlPanel, c);

      c.weightx = 1.0;
      c.weighty = 15.0;
      c.gridx = 0;
      c.gridy = 1;
      c.insets = new Insets(0, 5, 5, 5);
      pane.add((cardSampleViewerPanel), c);

      switch (ddsTopic.topicQos) {
         
      case DDS_QOS_PROFILE_3:

         ddsTopic.dataReaderMode = DDSTopic.DataReaderMode.TAKE;
         ddsTopic.viewMode = DDSTopic.ViewMode.TEXT_LIST;
         listMenuItem.setSelected(true);
         readerTakeMenuItem.setSelected(true);
         injectButton.setVisible(false);
         break;
         
         
      case DDS_QOS_PROFILE_1:
      case DDS_QOS_PROFILE_2:
      default:
         
         ddsTopic.dataReaderMode = DDSTopic.DataReaderMode.READ;
         ddsTopic.viewMode = DDSTopic.ViewMode.TREE_TABLE;
         treeMenuItem.setSelected(true);
         readerReadMenuItem.setSelected(true);
         injectButton.setVisible(true);
         break;
      }
      
      TopicManager topicManager = TopicManager.getInstance();
      
      // if collecting data default to using take
      if ((topicManager.GetDataCaptureMode() == 
         TopicManager.DataCaptureMode.DATA_LOGGER) ||
         (topicManager.GetDataCaptureMode() == 
            TopicManager.DataCaptureMode.SESSION_CAPTURE)) {
         
         ddsTopic.dataReaderMode = DDSTopic.DataReaderMode.TAKE;
         ddsTopic.viewMode = DDSTopic.ViewMode.TEXT_LIST;
         listMenuItem.setSelected(true);
         readerTakeMenuItem.setSelected(true);
         injectButton.setVisible(false);
      }

      propertyChangeSupport = new PropertyChangeSupport(this);
      propertyChangeSupport
            .addPropertyChangeListener(ddsTopic.topicReaderWriter);

      frame.setVisible(true);

      ddsTopic.autoUpdate = false;
      treeTableInitialized = false;
   }

   private void createCommonControlPanels(JPanel dataReaderPanel,
         JPanel viewModePanel, JPanel sampleControlPanel) {

      // Create radio button menu items
      treeMenuItem = new JRadioButtonMenuItem();
      listMenuItem = new JRadioButtonMenuItem();
      readerReadMenuItem = new JRadioButtonMenuItem();
      readerTakeMenuItem = new JRadioButtonMenuItem();

      // Create DataReader buttonGroup
      ButtonGroup dataReaderButtonGroup = new ButtonGroup();
      Border border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            "DataReader");

      // Populate DataReader panel
      dataReaderPanel.setBorder(border);
      dataReaderPanel
            .setLayout(new BoxLayout(dataReaderPanel, BoxLayout.Y_AXIS));
      readerReadMenuItem.setBorder(BorderFactory.createEmptyBorder());
      readerReadMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected)
               ddsTopic.dataReaderMode = DDSTopic.DataReaderMode.READ;
         }
      });
      readerReadMenuItem.setText(" Read    ");
      dataReaderButtonGroup.add(readerReadMenuItem);
      readerTakeMenuItem.setBorder(BorderFactory.createEmptyBorder());
      readerTakeMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected)
               ddsTopic.dataReaderMode = DDSTopic.DataReaderMode.TAKE;
         }
      });
      readerTakeMenuItem.setText(" Take    ");
      dataReaderButtonGroup.add(readerTakeMenuItem);
      dataReaderPanel.add(readerReadMenuItem);
      dataReaderPanel.add(readerTakeMenuItem);

      // Create ViewMode button group
      ButtonGroup viewModeButtonGroup = new ButtonGroup();
      Border viewModeBorder = BorderFactory
            .createTitledBorder(
                  BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                  "View Mode");

      // Populate ViewMode panel
      viewModePanel.setBorder(viewModeBorder);
      viewModePanel.setLayout(new BoxLayout(viewModePanel, BoxLayout.Y_AXIS));
      treeMenuItem.setBorder(BorderFactory.createEmptyBorder());
      treeMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected) {
               ddsTopic.viewMode = DDSTopic.ViewMode.TREE_TABLE;
               CardLayout cardLayout = (CardLayout) (cardSampleViewerPanel
                     .getLayout());
               cardLayout.show(cardSampleViewerPanel, "TREE_TABLE");
               treeMenuItem.setSelected(true);
               injectButton.setVisible(true);
               resizeButton.setVisible(true);
               publishButton.setVisible(true);
               clearTextListButton.setVisible(false);
            }
         }
      });
      treeMenuItem.setText(" Tree Table");
      viewModeButtonGroup.add(treeMenuItem);

      listMenuItem.setBorder(BorderFactory.createEmptyBorder());
      listMenuItem.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected) {
               ddsTopic.viewMode = DDSTopic.ViewMode.TEXT_LIST;
               CardLayout cardLayout = (CardLayout) (cardSampleViewerPanel
                     .getLayout());
               cardLayout.show(cardSampleViewerPanel, "TEXT_LIST");
               listMenuItem.setSelected(true);
               injectButton.setVisible(false);
               resizeButton.setVisible(false);
               publishButton.setVisible(false);
               clearTextListButton.setVisible(true);
            }
         }
      });
      listMenuItem.setText(" Text List");
      viewModeButtonGroup.add(listMenuItem);
      viewModePanel.add(treeMenuItem);
      viewModePanel.add(listMenuItem);

      // Populate Sample Control panel region
      // Auto Update checkbox
      JLabel autoUpdateLabel = new JLabel("Auto Update");
      autoUpdateCheckBox = new JCheckBox();
      autoUpdateCheckBox.setSelected(false);
      autoUpdateCheckBox.addItemListener(this);
      sampleControlPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      sampleControlPanel.add(autoUpdateCheckBox);
      autoUpdateLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
      sampleControlPanel.add(autoUpdateLabel);

      // Inject button
      injectButton = new JButton("Inject");
      injectButton.setEnabled(true);
      sampleControlPanel.add(injectButton);
      injectButton.addActionListener(this);
      
      // Publish button
      publishButton = new JButton("Publish");
      publishButton.setEnabled(false);
      sampleControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      sampleControlPanel.add(publishButton);
      publishButton.addActionListener(this);

      // Resize button
      resizeButton = new JButton("Resize");
      resizeButton.setEnabled(false);
      sampleControlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
      sampleControlPanel.add(resizeButton);
      resizeButton.addActionListener(this);
      
      // Clear text list  button
      clearTextListButton = new JButton("Clear All");
      clearTextListButton.setEnabled(true);
      sampleControlPanel.add(clearTextListButton);
      clearTextListButton.addActionListener(this);
   }

   private void createOutlineTreeTable(OricSymbolMap oricSymMap) {

      SymbolMapTreeModel treeMdl = new SymbolMapTreeModel(oricSymMap);
      org.netbeans.swing.outline.OutlineModel mdl;

      if ((outline != null) && (listSelectionListener != null)) {

         outline.getSelectionModel().removeListSelectionListener(
               listSelectionListener);
      }

      treeTablePanel.removeAll();

      outline = new org.netbeans.swing.outline.Outline();
      outline.setRenderDataProvider(new RenderSymbolMap(oricSymMap));
      outline.getTableHeader().setReorderingAllowed(false);
      outline.setColumnHidingAllowed(false);
      outline.setSelectionMode(0);
      
      listSelectionListener = new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {

            int row = outline.getSelectedRow();
            if (!e.getValueIsAdjusting() && (row > -1)
                  && ((String) outline.getValueAt(row, 1)).equals("ARRAY")) {
               resizeNodeSymbol = (String) outline.getValueAt(row, 0);
               if (!resizeButton.isEnabled())
                  resizeButton.setEnabled(true);
            } else {
               if (resizeButton.isEnabled())
                  resizeButton.setEnabled(false);
            }
         }
      };

      outline.getSelectionModel().addListSelectionListener(
            listSelectionListener);

      symbolRowModel = new SymbolRowModel();
      symbolRowModel.setSymbolMap(oricSymMap);
      oricSymMapCount = oricSymMap.getSymbolCount();

      mdl = DefaultOutlineModel.createOutlineModel(treeMdl, symbolRowModel,
            true);
      outline.setRootVisible(false);
      outline.setModel(mdl);
      outline.setCellSelectionEnabled(true);

      treeTablePanel.add(new JScrollPane(outline));
      treeTablePanel.revalidate();
      
      treeTableInitialized = true;
   }

   public void updateTreeTable(OricSymbolMap newOricSymMap) {

      // Create tree table on first call
      if (!treeTableInitialized) {
         createOutlineTreeTable(newOricSymMap);
         return;
      }

      // If the number of symbols has changed, the entire tree table
      // needs to be regenerated.
      if (newOricSymMap.getSymbolCount() != oricSymMapCount) {
         createOutlineTreeTable(newOricSymMap);
      } else {
         // Update model to new symbol map
         symbolRowModel.setSymbolMap(newOricSymMap);

         // Generate a table model event to refresh table
         TableModel tableModel = outline.getModel();
         TableModelEvent tableModelEvent = new TableModelEvent(tableModel);
         outline.tableChanged(tableModelEvent);
      }
   }

   public void updateTextTable(String sampleXml) {

      sampleTextList.writeXML(sampleXml);
   }

   public void shutdownReq() {

      ddsTopic.subscriberAttached = false;

      // Start the shutdown sequence for topic reader writer thread
      if (ddsTopic.topicReaderWriter != null)
         ddsTopic.topicReaderWriter.shutdownReq();

      // Start the shutdown sequence for the data logger thread
      if (ddsTopic.dataLogger != null)
         ddsTopic.dataLogger.shutdownReq();
      
      // Start the shutdown sequence for the session capture thread
      if (ddsTopic.sessionCapture != null)
         ddsTopic.sessionCapture.shutdownReq();      

      while (!ddsTopic.readEventHandlerFinished
            || !ddsTopic.topicReaderWriterFinished
            || !ddsTopic.dataLoggerFinished
            || !ddsTopic.sessionCaptureFinished) {
         try {
            Thread.sleep(200);
         } catch (InterruptedException e) {
         }
      }

      frame.dispose();
   }

   @Override
   public void actionPerformed(ActionEvent event) {

      if ((JButton) event.getSource() == publishButton) {

         propertyChangeSupport.firePropertyChange("ddsWriteEvent", 0,
               symbolRowModel.getOricSymbolMap().symMapToObj());
      } else if ((JButton) event.getSource() == injectButton) {

         String SEQ_HOLDER = "SeqHolder";
         Class<?> seqHolderClass = null;
         Object seqHolder = null;
         OricSymbolMap oricSymbolMap = new OricSymbolMap();

         try {
            seqHolderClass = Class.forName(ddsTopic.dataType + SEQ_HOLDER);
            seqHolder = seqHolderClass.newInstance();

            oricSymbolMap.instantiateObj(seqHolder);
            oricSymbolMap.objToSymMap(seqHolder);
         } catch (Exception e) {
            System.err.println("ERROR: SequenceHolder instantiation failed");
         }
         updateTreeTable(oricSymbolMap);
         publishButton.setEnabled(true);
      } else if ((JButton) event.getSource() == resizeButton) {

         ResizeBox resizeBox = new ResizeBox();
         int newArraySize = resizeBox.ShowDialog();

         // Resize array in symbol map and instantiate new object
         OricSymbolMap oricSymbolMap = symbolRowModel.getOricSymbolMap();
         oricSymbolMap.setFieldNumChildren(resizeNodeSymbol,
               Integer.toString(newArraySize));
         Object newObject = oricSymbolMap.symMapToObj();
         oricSymbolMap.instantiateObj(newObject);

         // Create new symbol map based on resized object
         OricSymbolMap newOricSymbolMap = new OricSymbolMap();
         newOricSymbolMap.objToSymMap(newObject);
         updateTreeTable(newOricSymbolMap);
      } else if ((JButton) event.getSource() == clearTextListButton) {
         sampleTextList.clearAll();
      }
   }

   @Override
   public void itemStateChanged(ItemEvent e) {

      if (e.getSource() == autoUpdateCheckBox) {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            if (publishButton != null)
               publishButton.setEnabled(false);
            if (resizeButton != null)
               resizeButton.setEnabled(false);
            if (injectButton != null)
               injectButton.setEnabled(false);
            ddsTopic.autoUpdate = true;
         } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            if (publishButton != null)
               publishButton.setEnabled(true);
            if (injectButton != null)
               injectButton.setEnabled(true);
            ddsTopic.autoUpdate = false;
         }
      }
   }
}