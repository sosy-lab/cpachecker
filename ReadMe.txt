CPAchecker Installation Requirements

Requirements for executing CPAchecker:
1. Source have to be preprocessed by CIL.
   Necessary flags: 
   --printCilAsIs

Requirements for building CPAchecker:
1. Install Java 1.6 SDK or higher.
   http://java.sun.com/
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work and compile with 1.5
   (may show degraded performance, though).
2. Install Eclipse 3.3 or higher.
   http://www.eclipse.org/
3. Install C/C++ Develoment Kit (platform and sdk) 4.0.3 or lower (LOWER).
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work with CDT 5
4. Add eclipse/plugins to your CLASSPATH.
   (Or add many many .jar files.)
5. Adapt the file .classpath to your directory locations.

For developers:
6. Install (e.g.) SubClipse - Eclipse SVN-Team Provider
   http://subclipse.tigris.org/
   Check out sourse code of CPAchecker from 
   URL: svn+ssh://cs-sel-02.cs.surrey.sfu.ca/localhome/dbeyer/SVN-software/cpachecker

Example of working installation (db 2008-11-28):
1. Java 1.6.0_10
2. Eclipse 3.4.1 (Ganymede)
3. CDT 4.0.3
