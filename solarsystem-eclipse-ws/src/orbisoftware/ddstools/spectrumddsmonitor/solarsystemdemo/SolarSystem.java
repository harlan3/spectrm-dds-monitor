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

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.*;

@SuppressWarnings("serial")
public class SolarSystem extends JApplet {

   private static final int width = 425;
   private static final int height = 425;

   private PlanetPublisher solarSystemPublisher;
   private boolean firstPaint;

   private ArrayList<PlanetModel> planetList = new ArrayList<PlanetModel>();
   private BufferedImage bufferedImage;
   private Graphics2D graphics2D;

   public void init() {

      PlanetModel.setScreenDimensions(width, height);
      setBackground(Color.black);
      firstPaint = true;

      // Initialize DDS Solar System publisher
      solarSystemPublisher = new PlanetPublisher();
      solarSystemPublisher.init();

      // Initialize Solar System
      initSolarSystem();
   }

   public void paint(Graphics g) {

      Graphics2D g2d = (Graphics2D) g;

      if (firstPaint) {

         bufferedImage = (BufferedImage) createImage(width, height);
         graphics2D = bufferedImage.createGraphics();

         firstPaint = false;
      }

      for (PlanetModel planet : planetList)
         planet.draw(graphics2D);

      g2d.drawImage(bufferedImage, 0, 0, this);
   }

   private void initSolarSystem() {

      planetList.add(new PlanetModel(0, "Sun", 0, 40, "yellow"));
      planetList.add(new PlanetModel(1, "Mercury", 30, 6, "red"));
      planetList.add(new PlanetModel(2, "Venus", 45, 12, "pink"));
      planetList.add(new PlanetModel(3, "Earth", 65, 15, "blue"));
      planetList.add(new PlanetModel(4, "Mars", 85, 12, "orange"));
      planetList.add(new PlanetModel(5, "Jupiter", 105, 25, "gray"));
      planetList.add(new PlanetModel(6, "Saturn", 130, 22, "lightGray"));
      planetList.add(new PlanetModel(7, "Uranus", 155, 12, "cyan"));
      planetList.add(new PlanetModel(8, "Neptune", 180, 11, "green"));
      planetList.add(new PlanetModel(9, "Pluto", 200, 5, "magenta"));
   }

   public static void main(String argv[]) {

      SolarSystem solarSystem = new SolarSystem();

      // Create a new jFrame with Solar System contentPane
      JFrame frame = new JFrame("Solar System");

      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            System.exit(0);
         }
      });
      frame.getContentPane().add(solarSystem);

      frame.setSize(new Dimension(width + 8, height + 30));
      frame.setVisible(true);

      solarSystem.init();

      while (true) {

         solarSystem.repaint();

         // Publish the Solar System to DDS
         solarSystem.solarSystemPublisher.publish(solarSystem.planetList);

         try {
            Thread.sleep(30);
         } catch (InterruptedException e1) {
         }
      }
   }
}
