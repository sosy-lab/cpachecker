CPAchecker Installation
=======================

For information on how to run CPAchecker, see [`README.md`](README.md).

Install CPAchecker -- Binary
----------------------------

1. Install a Java Runtime Environment which is at least Java 8 compatible
   (e.g., Oracle JRE, OpenJDK).
   Cf. http://java.oracle.com/ or install a package from your distribution.
   (Ubuntu: `sudo apt-get install openjdk-8-jre`)
   If you have multiple JVMs installed, consider making this the default JVM,
   otherwise you will need to specify the JVM when running CPAchecker.
   (Ubuntu: `sudo update-alternatives --config java`)

2. Extract the content of the CPAchecker zip or tar file into a directory of your choice.

Install CPAchecker -- Source
----------------------------

1. Install a Java SDK which is at least Java 8 compatible
   (e.g., Oracle JDK, OpenJDK).
   Cf. http://java.oracle.com/ or install a package from your distribution.
   (Ubuntu: `sudo apt-get install openjdk-8-jdk`)
   If you have multiple JDKs installed, make sure that the commands `java`
   and `javac` call the respective Java 8 binaries,
   so put them in your PATH or change the system-wide default JDK.
   (Ubuntu: `sudo update-alternatives --config java; sudo update-alternatives --config javac`)

2. Install `ant` (anything since version 1.7 should be ok).
   (Ubuntu: `sudo apt-get install ant`)

3. Install Subversion.
   (Ubuntu: `sudo apt-get install subversion`)

4. Checkout CPAchecker from SVN repository.
   URL: https://svn.sosy-lab.org/software/cpachecker/trunk
   URL (read-only GIT mirror): https://github.com/sosy-lab/cpachecker

5. Run `ant` in CPAchecker directory to build CPAchecker.
   When building CPAchecker for the first time, this will automatically
   download all needed libraries.
   If you experience problems, please check the following items:
   - If you have incompatible versions of some libraries installed on your system,
     the build might fail with NoSuchMethodErrors or similar exceptions.
     In this case, run `ant -lib lib/java/build`.
   - If the build fails due to compile errors in AutomatonScanner.java or FormulaScanner.java,
     you have a too-old version of JFlex installed.
     In this case, run `ANT_TASKS=none ant` to use our supplied JFlex
     instead of the one installed on the system.
   - If the build fails because the class `org.apache.ivy.ant.BuildOBRTask` cannot be found,
     this is probably caused by an old Ivy version installed on your system.
     Please try uninstalling Ivy.

(For building CPAchecker within Eclipse, cf. [`doc/Developing.md`](doc/Developing.md).)
