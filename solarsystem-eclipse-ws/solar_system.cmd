echo off

REM Add the required jars to classpath
set CP=%OSPL_HOME%\jar\dcpssaj.jar;.\lib\solar_system.jar

REM Add DDS idl generated data classes/jars here
set CP=%CP%;.\lib\DDSSolarSystem.jar

java -cp %CP% orbisoftware.ddstools.spectrumddsmonitor.solarsystemdemo.SolarSystem

