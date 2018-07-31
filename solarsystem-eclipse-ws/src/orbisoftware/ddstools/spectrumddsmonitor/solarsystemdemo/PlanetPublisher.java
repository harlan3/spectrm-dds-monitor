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

import java.util.ArrayList;

public class PlanetPublisher {

   private DDSSolarSystem.PlanetDataWriter dataWriter;

   public void init() {

      String myDomain = null;
      DDS.DomainParticipantFactory dpf;
      DDS.DomainParticipant dp;
      DDS.Publisher p;

      DDS.DomainParticipantQosHolder dpQos;
      DDS.TopicQosHolder tQos;
      DDS.PublisherQosHolder pQos;
      DDS.DataWriterQosHolder dwQos;
      DDS.Topic solarSystemTopic;

      DDSSolarSystem.PlanetTypeSupport ts;

      // Hook up to DDS
      dpQos = new DDS.DomainParticipantQosHolder();
      tQos = new DDS.TopicQosHolder();
      pQos = new DDS.PublisherQosHolder();
      dwQos = new DDS.DataWriterQosHolder();

      dpf = DDS.DomainParticipantFactory.get_instance();
      dpf.get_default_participant_qos(dpQos);

      dp = dpf.create_participant(myDomain, dpQos.value, null,
            DDS.STATUS_MASK_NONE.value);
      dpQos = null;

      // Create publisher
      dp.get_default_publisher_qos(pQos);
      pQos.value.partition.name = new String[1];
      pQos.value.partition.name[0] = "";
      p = dp.create_publisher(pQos.value, null, DDS.STATUS_MASK_NONE.value);
      pQos = null;

      // Get default DataReader and DataWriter QoS settings
      p.get_default_datawriter_qos(dwQos);

      // Get default Topic Qos settings
      dp.get_default_topic_qos(tQos);

      // Create Topic
      ts = new DDSSolarSystem.PlanetTypeSupport();
      ts.register_type(dp, "DDSSolarSystem::Planet");

      solarSystemTopic = dp.create_topic("Planet", "DDSSolarSystem::Planet",
            tQos.value, null, DDS.STATUS_MASK_NONE.value);

      // Create datawriter
      dataWriter = DDSSolarSystem.PlanetDataWriterHelper.narrow(p
            .create_datawriter(solarSystemTopic, dwQos.value, null,
                  DDS.STATUS_MASK_NONE.value));

      if (dataWriter == null)
         System.err.println("ERROR: DDS Connection failed");
   }

   public void publish(ArrayList<PlanetModel> planetList) {

      for (int i = 0; i < planetList.size(); i++)
         dataWriter.write(planetList.get(i).getDDSPlanet(),
               DDS.HANDLE_NIL.value);
   }
}
