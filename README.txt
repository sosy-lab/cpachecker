Getting Started with CPAchecker
===============================
More details can be found in doc/*.txt

Prepare Programs for Verification by CPAchecker
-----------------------------------------------

   Sources have to be preprocessed by CIL
   (http://hal.cs.berkeley.edu/cil/, mirror at http://www.cs.berkeley.edu/~necula/cil/).
   Necessary flags:
   --dosimplify --printCilAsIs --save-temps --domakeCFG
   Possibly necessary flags:
   --dosimpleMem
   Comments:
   --save-temps saves files to the current directory, a different directory can
   be specified by using --save-temps=<DIRECTORY>


Verifying a Program with CPAchecker
-----------------------------------

0. You need a Java Runtime Environment which is at least Java 6 compatible
   (e.g., Sun/Oracle JRE, OpenJDK).

1. Choose a source code file that you want to be checked.
   Several types of example programs can be found in test/programs/
   If you use your own program, remember to pre-process it with CIL (see above).
   Example: test/programs/simple/loop1.c

2. If you want to enable certain analyses like predicate analysis,
   choose a configuration file. This file defines for example which CPAs are used.
   Standard configuration files can be found in the directory test/config/.
   Example: test/config/explicitAnalysis.properties
   The configuration options used in this file are explained in doc/Configuration.txt.

3. Choose a specification file (you may not need this for some CPAs).
   The standard configuration files use test/config/automata/ErrorLocationAutomaton.txt
   as the default specification. With this one, CPAchecker will look for labels
   named "ERROR" and assertions in the source code file.
   Other examples for specifications can be found in test/config/automata/.

4. Execute "scripts/cpa.sh [ -config <CONFIG_FILE> ] [ -spec <SPEC_FILE> ] <SOURCE_FILE>"
   Either a configuration file or a specification file needs to be given.
   The current directory should be the CPAchecker project directory.
   Additional command line switches are described in doc/Configuration.txt.
   Example: scripts/cpa.sh -config test/config/explicitAnalysis.properties test/programs/simple/loop1.c
   This example can also be abbreviated to:
   scripts/cpa.sh -explicitAnalysis test/programs/simple/loop1.c

5. Additionally to the console output, there will be several output files in test/output/:
     ART.dot: Visualization of abstract reachability tree (Graphviz format)
     cfa*.dot: Visualization of control flow automaton (Graphviz format)
     counterexample.msat: Formula representation of the error path
     ErrorPath.txt: A path through the program that leads to an error
     ErrorPathAssignment.txt: Assignments for all variables on the error path.
     predmap.txt: Predicates used by predicate analysis to prove program safety
     reached.txt: Dump of all reached abstract states
     Statistics.txt: Time statistics (can also be printed to console with "-stats")
   Note that not all of these files will be available for all configurations.
   Also some of these files are only produced if an error is found (or vice-versa).
   CPAchecker will overwrite files in this directory!
   These files may be used to generate a report that can be viewed in a browser.
   Cf. BuildReport.txt for this.


