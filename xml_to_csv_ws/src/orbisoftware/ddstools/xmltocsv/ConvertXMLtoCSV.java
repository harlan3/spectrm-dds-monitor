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

public class ConvertXMLtoCSV {

   public static void main(String argv[]) {

      if (argv.length != 1) {
         System.out.println("Usage: xmltocsv filename");
         System.out
               .println("Generates a csv file from an xml file.");
         System.exit(-1);
      }
      
      String fileName = argv[0];

      try {
         XMLReader parser = org.xml.sax.helpers.XMLReaderFactory
               .createXMLReader();

         // Create a new instance and register it with the parser
         DefineTableColumns defineTableColumns = new DefineTableColumns();
         parser.setContentHandler(defineTableColumns);

         // First pass, parse table column headers
         parser.parse(fileName);

         // Generate CSV filename
         String csvFileName = fileName.replace(".xml", ".csv");

         GenerateCSVFile generateCSVFile = new GenerateCSVFile(
               defineTableColumns.getSortedHeaders(), csvFileName);
         parser.setContentHandler(generateCSVFile);

         // Second pass, generate CSV File
         parser.parse(fileName);

         generateCSVFile.closeFile();

      } catch (Exception exception) {
         exception.printStackTrace();
      }
   }
}
