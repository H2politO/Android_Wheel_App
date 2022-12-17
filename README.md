# Wheel Application Project

## Description
The project was made for read information from USB serial device, and upload data in claud using MQTT protocol.
For read the info from usb, use [USB-SERIAL-FOR-ANDROID](https://github.com/mik3y/usb-serial-for-android), and for
communicate with mqtt protocol use maven repository.

#### Help
For using properly mqtt:  
    - add in gradle.properties -> enebleJetifire=true  
    - add in settings.gradle -> mavenCentral() & maven{url...}  
    - add timber  
    - modify the implementation of serviceLibrary-release.aar with correct directory