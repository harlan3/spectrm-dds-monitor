/*
 *  Spectrum DDS data monitor tool
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

package orbisoftware.ddstools.spectrumddsmonitor.sampleviewer;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.Date;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import javax.swing.JTextPane;

@SuppressWarnings("serial")
public class SampleTextList extends JTextPane {

   enum XmlToken {
      NONE, LEFT_TOKEN, RIGHT_TOKEN, TEXT
   }
   
   // Maximum length of buffer
   private int MAX_BUFFER = 102400;
   
   // Remove first 20% of buffer when MAX_BUFFER is reached
   private double BUFFER_REDUCTION = 0.2;

   public void writeXML(String xmlContent) {

      char xmlContentCharArray[] = xmlContent.toCharArray();
      Color tagColor = new Color(24, 147, 145);

      int cursor = 0;
      int startMarker = 0;
      XmlToken xmlToken = XmlToken.NONE;
      StyledDocument doc = getStyledDocument();

      append(Color.BLUE, " Sample Timestamp: " + new Timestamp(new Date().getTime()) + " ");
      
      while (cursor < xmlContentCharArray.length) {

         if (xmlContentCharArray[cursor] == '<') {

            if (xmlToken == XmlToken.TEXT) {
               append(Color.BLACK, xmlContent.substring(startMarker, cursor));
            }

            xmlToken = XmlToken.LEFT_TOKEN;
            startMarker = cursor;

         } else if (xmlContentCharArray[cursor] == '>') {

            append(tagColor, xmlContent.substring(startMarker, cursor + 1));
            xmlToken = XmlToken.NONE;

         } else if (xmlToken == XmlToken.NONE) {

            startMarker = cursor;
            xmlToken = XmlToken.TEXT;
         }
         cursor++;
      }

      append(Color.BLACK, "\n");

      if (doc.getLength() > MAX_BUFFER)
         try {
            doc.remove(0, (int)(MAX_BUFFER * BUFFER_REDUCTION));
         } catch (Exception e) {
            e.printStackTrace();
         }
   }

   public void clearAll() {
      
      StyledDocument doc = getStyledDocument();
      try {
         doc.remove(0, doc.getLength());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   private void append(Color color, String string) {

      StyleContext styleContext = StyleContext.getDefaultStyleContext();
      AttributeSet attributeSet = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

      int len = getDocument().getLength();
      setCaretPosition(len);
      setCharacterAttributes(attributeSet, false);
      replaceSelection(string);
   }
}
