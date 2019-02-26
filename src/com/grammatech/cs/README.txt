The Java API is in beta at this stage.  Please report any problems to
support@grammatech.com.

There are two ways of running Java code:

1.  By command line.  To run some java method after loading the
CodeSurfer project named 'test':

  Windows:
    csurf\bin\csurf -nogui -java c:\plugin_classpath PluginClassName 'C:\Program Files\Java\jre6\bin\server\jvm.dll' arg1 arg2 -end-java test

  Other:
    csurf/bin/csurf -nogui -java /plugin/classpath PluginClassName /usr/lib/jvm/java-6-openjdk-amd64/jre/lib/amd64/server/libjvm.so arg1 arg2 -end-java test
    
  1st Argument: Classpath given to java.  Use ; or : depending on your
  OS to separate multiple entries.

  2nd Argument: The name of the class whose main function will be
  invoked.  Should have signature:
    public static void main(String[] args) {...}

  3rd Argument: The JVM DLL or shared object to use.

  Subsequent Arguments: Passed to main in the args array.

2.  Create a scheme plugin that loads your java plugin.  The scheme
    plugin can invoke the java plugin's main function like so:

    (java-main "c:\\plugin_classpath" "PluginClassName" "C:\\Program Files\\Java\\jre6\\bin\\server\\jvm.dll" "arg1" "arg2")

    The arguments are exactly the same as with method #1 above.

Here is a very simple complete plugin:

import com.grammatech.cs.*;
import java.lang.*;

public class PluginClassName{
    public static void main(String[] args) {
        try{
            project p = project.current();
            System.out.println(p.procedures_vector().get(0));
        }catch(result r){
            System.out.println("Oops: " + r);
        }
    }
}
    
  

There is fairly little documentation for the Java API at this point.
The interface can be seen by browsing the csurf/src/api/java/*.java
files in this directory.  It has roughly the same interface as the
python and C++ APIs.  The C++ API is a shallow wrapper around the C
API, which is fairly well documented.  The C++ API can be found in
csurf/include/*.hpp.
