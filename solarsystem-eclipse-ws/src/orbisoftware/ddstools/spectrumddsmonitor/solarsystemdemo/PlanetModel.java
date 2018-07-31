/*
 *  Solar System Demo
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

package orbisoftware.ddstools.spectrumddsmonitor.solarsystemdemo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.Field;

public class PlanetModel {

   static int width;
   static int height;

   private DDSSolarSystem.Planet ddsPlanet;

   public PlanetModel(int index, String planetName, double orbitalRadius,
         double planetSize, String planetColor) {

      ddsPlanet = new DDSSolarSystem.Planet();

      ddsPlanet.planetId = index;
      ddsPlanet.planetName = planetName;
      ddsPlanet.orbitalRadius = orbitalRadius;
      ddsPlanet.planetSize = planetSize;
      ddsPlanet.planetColor = planetColor;

      if (ddsPlanet.orbitalRadius == 0.0)
         ddsPlanet.orbitalVelocity = 0.0;
      else
         ddsPlanet.orbitalVelocity = Math.sqrt(5000.0 / Math.pow(orbitalRadius,
               2));
   }

   public static void setScreenDimensions(int newWidth, int newHeight) {
      width = newWidth;
      height = newHeight;
   }

   public DDSSolarSystem.Planet getDDSPlanet() {
      return ddsPlanet;
   }

   public void draw(Graphics2D g2) {

      // Erase previous
      g2.setPaint(Color.black);
      drawOrbit(g2);
      drawPlanet(g2);

      // Update Orbit
      updateOrbit();

      // Draw
      g2.setPaint(Color.white);
      drawOrbit(g2);
      g2.setPaint(getColor(ddsPlanet.planetColor));
      drawPlanet(g2);
   }

   private void drawPlanet(Graphics2D g2) {

      double xOffset = ddsPlanet.xPos - ddsPlanet.planetSize / 2.0;
      double yOffset = ddsPlanet.yPos - ddsPlanet.planetSize / 2.0;

      Ellipse2D myEllipse = new Ellipse2D.Double(xOffset, yOffset,
            ddsPlanet.planetSize, ddsPlanet.planetSize);
      g2.fill(myEllipse);
   }

   private void drawOrbit(Graphics2D g2) {

      double orbitalDiameter = ddsPlanet.orbitalRadius * 2.0;
      int offsetX = (int) (width / 2.0 - ddsPlanet.orbitalRadius);
      int offsetY = (int) (height / 2.0 - ddsPlanet.orbitalRadius);

      Ellipse2D orbit = new Ellipse2D.Double(offsetX, offsetY, orbitalDiameter,
            orbitalDiameter);

      g2.setStroke(new BasicStroke(0));
      g2.draw(orbit);
   }

   private Color getColor(String colorName) {

      try {
         Field field = Class.forName("java.awt.Color").getField(colorName);
         return (Color) field.get(null);
      } catch (Exception e) {
         return null;
      }
   }

   private void updateOrbit() {

      if (ddsPlanet.orbitalRadius == 0) {
         ddsPlanet.theta = 0;
         ddsPlanet.xPos = width / 2;
         ddsPlanet.yPos = height / 2;
      } else {
         ddsPlanet.theta = ddsPlanet.theta + ddsPlanet.orbitalVelocity
               / ddsPlanet.orbitalRadius;
         ddsPlanet.xPos = width / 2 - ddsPlanet.orbitalRadius
               * Math.sin(ddsPlanet.theta);
         ddsPlanet.yPos = height / 2 - ddsPlanet.orbitalRadius
               * Math.cos(ddsPlanet.theta);
      }
   }
}
