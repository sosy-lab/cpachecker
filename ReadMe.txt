CPAchecker Installation Requirements

Requirements for executing CPAchecker:
1. Sources have to be preprocessed by CIL.
   Necessary flags: 
   --printCilAsIs

Requirements for building CPAchecker:
1. Install Java 1.6 SDK or higher.
   http://java.sun.com/
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work and compile with 1.5
   (may show degraded performance, though).
2. Install Eclipse 3.3 or 3.4
   http://www.eclipse.org/
   You need the JDT and the "Eclipse Plug-in Development Environment" package (PDE).
3. Install C/C++ Development Kit (platform and sdk) 4.0.3 or lower (LOWER).
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work with CDT 5
   You need only the "Eclipse C/C++ Development Tools SDK" package.
4. Or install Eclipse 3.5 and CDT 6.0 (CDT 4.0 won't install on Eclipse 3.5)
   and apply the cdt-6-compatibility patch found in the project root. 

For building in Eclipse:
4. Install (e.g.) SubClipse - Eclipse SVN-Team Provider
   http://subclipse.tigris.org/
5. Create new project from SVN repository
   URL: svn+ssh://svn.sosy-lab.org/repos/RC-software/cpachecker/trunk
6. If your system is not 32bit Linux, you will have to change the paths to the
   native libraries in the .classpath file

Running it:
6. Choose a configuration file and a source code file
   Example: benchmarks-explicit/config/explicitAnalysisInf.properties
            benchmarks-explicit/working-set/ext/loop1.c
   Check that the configuration file does not contain any non-existent paths 
7. Running it from Eclipse:
   Create a run configuration with main class "cmdline.CPAMain" and program
   arguments "-config <CONFIG_FILE> <SOURCE_FILE>"
Or:
7. Running it from command line:
   Execute "test/scripts/simple/cpa.sh -config <CONFIG_FILE> <SOURCE_FILE>"
   You need to edit this script, if your Eclipse is not in ~/eclipse,
   /opt/eclipse or ~/Desktop/eclipse


Sources of binaries provided with the distribution/SVN:
- libJOct.so: Use steps similar to compileOctLib.sh after downloading and
  installing the octagon library (http://www.di.ens.fr/~mine/oct/ merged into
  APRON with different interfaces)
- javabdd-1.0b2.jar, libcudd.so: See
  http://javabdd.sourceforge.net/compiling.html
- Simplify: http://kind.ucd.ie/products/opensource/Simplify/
- mathsat.jar: Source code provided with the archive
- others: Unknown (MT)

Examples of working installation:
db 2008-11-28:
0. x86 32bit
1. Java 1.6.0_10
2. Eclipse 3.4.1 (Ganymede)
3. CDT 4.0.3

pwendler 2009-08-25:
0. x86 32bit Linux
1. Sun Java 1.6.0_14
2. Eclipse 3.5.0 (Galileo)
4. CDT 6.0.0