# ACE Client

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle

This project also depends on the ace-java (https://bitbucket.org/sebastian_echeverria/ace-java-postgres) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will find them when resolving its dependencies.
 
## Configuration
No configuration is needed. Optional parameters are available in "config.json":

 * id: the ID or name the client will use
 * credentials_file: the file used to store credentials. If you want to clear previous pairing credentials, simply delete the file indicated in this field.
   
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.client.gui.FXApplication. This starts the GUI. A simple way to start it from gradle is with `./gradlew run`

Retrieved tokens are stored in tokens.json. To clear all stored tokens, simply delete this file.

For more information, see https://github.com/SEI-TAS/ace-client/wiki
