# Keller Lab Block file type (.klb) - JNI Library #

## Java Native Interface (JNI) ##

The KLB API is exposed on the Java-side through a JNI wrapper, included in the "javaWrapper" subfolder. It can be build with maven, includes compiled native libraries for Windows and Linux (both 64-bit) and will eventually be available as an artifact on a Maven repository. ImageJ users on supported platforms can simply install KLB support by following the update site (see below).


## Install via ImageJ update site

KLB and its ImageJ integration are available through an ImageJ update site at http://sites.imagej.net/SiMView/. Follow these [instructions](http://wiki.imagej.net/How_to_follow_a_3rd_party_update_site) on how to follow an update site. Currently supported platforms are Windows and Linux, both 64-bit. Users on other platforms have to build the native libraries first.

## Build JNI library from source
  - install Maven
  - navigate to the javaWrapper subfolder
  - run "mvn clean package"
  - the JAR file will be built at "javaWrapper/target/klb-[version].jar"
