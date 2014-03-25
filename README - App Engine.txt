Using CPAchecker on Google App Engine
=======================================

CPAchecker can be run on Google App Engine. It is being developed alongside the
stand-alone project. For the time being all associated files are kept
separate from the default build process. To develop, compile and run the App
Engine project you need to install its dependencies first.

Installation
============

Execute the following command to retrieve all dependencies and build CPAchecker
for Google App Engine:

ant -f gae-build.xml build

Please be aware that the App Engine SDK will be downloaded which might take some
time since it is about 140 MB in size.

Development
===========

If you use Eclipse or some other IDE add the following JARs to the classpath:

lib/gae/**/*.jar
lib/appengine-java-sdk-1.9.1/lib/user/*.jar
lib/appengine-java-sdk-1.9.1/lib/impl/appengine-api.jar
lib/appengine-java-sdk-1.9.1/lib/impl/appengine-api-stubs.jar
lib/appengine-java-sdk-1.9.1/lib/impl/appengine-api-labs.jar
lib/appengine-java-sdk-1.9.1/lib/shared/servlet-api.jar
lib/appengine-java-sdk-1.9.1/lib/testing/appengine-testing.jar

The Eclipse classpath excludes the App Engine sources by default.
Thus you need to remove "org/sosy_lab/cpachecker/appengine" from the excluded paths.

You can start a development server by executing:

ant -f gae-build.xml server

It will be available at http://localhost:8888


Deployment
==========

To deploy the application to Google App Engine use the following Ant target:

ant -f gae-build.xml deploy

Make sure to setup OAuth2 before:
https://developers.google.com/appengine/docs/java/tools/uploadinganapp#Passwordless_Login_with_OAuth2

For more control over the deployment process and other administrative work
you can use the tool provided by Google:

./lib/appengine-java-sdk-*/bin/appcfg.sh
