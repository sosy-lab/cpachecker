The Java class that 'CFloatNativeAPI.h' is generated of resides in the
'org.sosy_lab.cpachecker.util.floatingpoint' package.

If you want to change the API remember to apply the changes accordingly in
both files.

If you want introduce major changes to the API, do it in the .java, compile
the .class and generate a new 'CFloatNativeAPI.h' via javah from it.

Remember to adjust in the .c file the java class paths for the used java classes,
when you decide to move or mutate the package.


Compile a new shared library object by running 'compile.sh', after setting
your 'JAVA_HOME' environment variable to your respective jvm, e.g.,
'/usr/lib/jvm/java-8-openjdk-amd64'.
