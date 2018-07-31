/*
 *  XML to CSV Converter for Spectrum log files
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

package orbisoftware.ddstools.xmltocsv;

import org.xml.sax.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GenerateCSVFile implements ContentHandler {

   // Contains the running sample count
   private int sampleCount = 0;
   
   // Contains the tree depth of an element
   private int elementDepth = 0;

   // Contains the element nodes defining the tree path to an element
   private ArrayList<String> elementArrayList = new ArrayList<String>();

   // Contains the array element count for a given tree depth
   private HashMap<Integer, Integer> arrayElementCountHashMap = new HashMap<Integer, Integer>();

   // Element name for individual array items
   private final String ARRAY_ELEMENT = "item";

   // Element name for individual row items
   private final String ROW_ELEMENT = "sample";

   // Column header maps element paths to column indices
   private HashMap<String, Integer> columnHeaderMap = new HashMap<String, Integer>();

   // Reference to sortedHeaders arraylist
   private ArrayList<String> sortedHeaders = null;

   // Row Content
   private HashMap<Integer, String> rowContent = new HashMap<Integer, String>();

   // CSV Filewriter
   private FileWriter fileWriter;
   private BufferedWriter csvWriter;

   public GenerateCSVFile(ArrayList<String> sortedHeaders, String csvFileName) {

      try {
         fileWriter = new FileWriter(csvFileName);
         csvWriter = new BufferedWriter(fileWriter);
      } catch (IOException e) {
         e.printStackTrace();
      }

      this.sortedHeaders = sortedHeaders;

      for (int i = 0; i < sortedHeaders.size(); i++) {
         columnHeaderMap.put(sortedHeaders.get(i), i);
      }

      rowContent.clear();
      elementArrayList.clear();
      arrayElementCountHashMap.clear();

      writeTableHeader();
   }

   public void closeFile() {
      try {
         csvWriter.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void setDocumentLocator(Locator locator) {
   }

   public void startDocument() throws SAXException {
   }

   public void endDocument() throws SAXException {

   }

   public void startPrefixMapping(String prefix, String uri)
         throws SAXException {
   }

   public void endPrefixMapping(String prefix) throws SAXException {
   }

   public void startElement(String uri, String localName, String qName,
         Attributes atts) throws SAXException {
      
      if (localName.equals(ARRAY_ELEMENT)) {
         int arrayElementCount;
         try {
            arrayElementCount = arrayElementCountHashMap.get(elementDepth);
         } catch (Exception e) {
            arrayElementCount = 1;
         }
         localName += Integer.toString(arrayElementCount);
         arrayElementCount++;
         arrayElementCountHashMap.put(elementDepth, arrayElementCount);
      }

      elementArrayList.add(localName);
      elementDepth++;
   }

   public void writeTableHeader() {
      try {
         for (int i = 0; i < sortedHeaders.size(); i++) {

            String content = sortedHeaders.get(i);

            if (content != null) {
               csvWriter.write(content);
            }

            if (i != sortedHeaders.size() - 1)
               csvWriter.write(",");
            else
               csvWriter.write("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void writeTableRow() {
      try {
         for (int i = 0; i < sortedHeaders.size(); i++) {

            String content = rowContent.get(i);

            // Strip leading and trail spaces and carriage returns
            if (content != null) {
               content = content.replaceAll("^\\s+", "");
               content = content.replaceAll("\\s+$", "");
               content = content.replaceAll("\r", "");
               content = content.replaceAll("\n", "");
            }

            if ((content != null) && !content.equals("")) {
               csvWriter.write(content);
            }

            if (i != sortedHeaders.size() - 1)
               csvWriter.write(",");
            else
               csvWriter.write("\n");
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void endElement(String uri, String localName, String qName)
         throws SAXException {

      if (!localName.equals(ARRAY_ELEMENT)) {
         arrayElementCountHashMap.put(elementDepth, 1);
      }

      if (localName.equals(ROW_ELEMENT)) {
         sampleCount++;
         rowContent.put(0, Integer.toString(sampleCount));
         writeTableRow();
         rowContent.clear();
      }

      elementArrayList.remove(elementArrayList.size() - 1);
      elementDepth--;
   }

   public void characters(char ch[], int start, int length) throws SAXException {

      StringBuffer elementPath = new StringBuffer();
      int columnIndex = -1;

      for (int i = 1; i < elementDepth; i++)
         elementPath.append("/" + elementArrayList.get(i));

      try {
         columnIndex = columnHeaderMap.get(elementPath.toString());
      } catch (Exception e) {

      }

      if (columnIndex == -1)
         return;

      String content = String.copyValueOf(ch, start, length);
      rowContent.put(columnIndex, content);
   }

   public void ignorableWhitespace(char ch[], int start, int length)
         throws SAXException {
   }

   public void processingInstruction(String target, String data)
         throws SAXException {
   }

   public void skippedEntity(String name) throws SAXException {
   }
}
