Using CPAchecker with Google App Engine
=======================================

Currently CPAchecker is being ported to run on Google's App Engine.
For the time being all associated files will be kept separate from the default
build process. Therefore to develop, compile and run the App Engine code you
need to install the according JARs and setup the classpath accordingly.

Please note that this project is under development. It might and will frequently
change in backward incompatible ways. Things will break and might not always
work as expected.

Installation
============

To install the necessary JARs run the following command. Please be aware that
the App Engine SDK will be downloaded which might take a while since it is about
140M in size.

ant -f gae-build.xml gae-install

Afterwards you need to add the following JARs to the classpath:
lib/gae/*.jar
lib/appengine-java-sdk-1.8.8/lib/user/*.jar
lib/appengine-java-sdk-1.8.8/lib/impl/appengine-api.jar
lib/appengine-java-sdk-1.8.8/lib/impl/appengine-api-stubs.jar
lib/appengine-java-sdk-1.8.8/lib/impl/appengine-api-stubs.jar
lib/appengine-java-sdk-1.8.8/lib/impl/appengine-api-labs.jar
lib/appengine-java-sdk-1.8.8/lib/testing/appengine-testing.jar

Also all App Engine related source code is excluded from the classpath by
default. Therefore you need to remove "org/sosy_lab/cpachecker/appengine/"
from the exclusion list.


Compiling and Running
=====================

To compile run one of the following commands. The first one will only compile
the classes while the second one will also fire up a web server.

ant -f gae-build.xml gae-compile
ant -f gae-build.xml gae-runserver