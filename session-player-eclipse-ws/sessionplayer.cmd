echo off

REM Add the required jars to classpath
set CP=%OSPL_HOME%\jar\dcpssaj.jar;.\lib\sessionplayer.jar;.\lib\outline.jar;.\lib\gson-1.7.jar

REM Add DDS idl generated data classes/jars here
set CP=%CP%;.\lib\DDSSolarSystem.jar

java -cp %CP% orbisoftware.ddstools.sessionplayer.SessionPlayer

