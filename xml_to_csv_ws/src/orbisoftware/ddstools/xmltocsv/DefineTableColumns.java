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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class DefineTableColumns implements ContentHandler {

   // Contains the tree depth of an element
   private int elementDepth = 0;

   // Contains the element nodes defining the tree path to an element
   private ArrayList<String> elementArrayList = new ArrayList<String>();

   // Contains the array element count for a given tree depth
   private HashMap<Integer, Integer> arrayElementCountHashMap = new HashMap<Integer, Integer>();

   // Contains the table column header set
   private HashSet<String> tableHeaderHashSet = new HashSet<String>();

   // Contains the sorted table column headers
   private ArrayList<String> sortedHeaders = new ArrayList<String>();

   // Element name for individual array items
   private final String ARRAY_ELEMENT = "item";

   public DefineTableColumns() {

      elementArrayList.clear();
      arrayElementCountHashMap.clear();
      tableHeaderHashSet.clear();
      sortedHeaders.clear();
   }

   public ArrayList<String> getSortedHeaders() {
      return sortedHeaders;
   }

   public void setDocumentLocator(Locator locator) {
   }

   public void startDocument() throws SAXException {
   }

   public void endDocument() throws SAXException {

      sortedHeaders.addAll(tableHeaderHashSet);
      Collections.sort(sortedHeaders);
   }

   public void startPrefixMapping(String prefix, String uri)
         throws SAXException {
   }

   public void endPrefixMapping(String prefix) throws SAXException {
   }

   public void startElement(String uri, String localName, String qName,
         Attributes atts) throws SAXException {

      StringBuffer elementPath = new StringBuffer();

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

      for (int i = 1; i < elementDepth; i++)
         elementPath.append("/" + elementArrayList.get(i));

      if (!elementPath.toString().equals(""))
         tableHeaderHashSet.add(elementPath.toString());
   }

   public void endElement(String uri, String localName, String qName)
         throws SAXException {

      if (!localName.equals(ARRAY_ELEMENT)) {
         arrayElementCountHashMap.put(elementDepth, 1);
      }

      elementArrayList.remove(elementArrayList.size() - 1);
      elementDepth--;
   }

   public void characters(char ch[], int start, int length) throws SAXException {

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
