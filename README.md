#ACE Client

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle

This project also depends on the ace-java (https://bitbucket.org/lseitz/ace-java) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) 
libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will
find them when resolving its dependencies.
 
## Configuration
No configuration is needed, but the ID or name the client will use, and the file used to store credentials, can be
changed in "config.json".
 
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.client.gui.FXApplication. This starts the GUI.
