CPAchecker Installation Requirements

Requirements for executing CPAchecker:
0. Sources have to be preprocessed by CIL (http://hal.cs.berkeley.edu/cil/).
   Necessary flags: 
   --dosimplify --printCilAsIs --save-temps

Requirements for building CPAchecker:
1. Install Java 1.6 SDK or higher.
   http://java.sun.com/
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work and compile with 1.5
   (may show degraded performance, though).
2. Install Eclipse 3.3, 3.4 or 3.5
   http://www.eclipse.org/
   You need the JDT and the "Eclipse Plug-in Development Environment" package (PDE).
3. Install the C/C++ Development Kit for your Eclipse version
   (tested with CDT 4.0 and CDT 6.0).   
   Or contact Michael Tautschnig <tautschnig@forsyte.de> to
   obtain patches to make it work with CDT 5
   You need to install only the "Eclipse C/C++ Development Tools SDK" package.

For building in Eclipse:
4. Install (e.g.) SubClipse - Eclipse SVN-Team Provider
   http://subclipse.tigris.org/
5. Create new project from SVN repository
   URL: svn+ssh://svn.sosy-lab.org/repos/RC-software/cpachecker/trunk
6. If your system is not 32bit Linux, you will have to change the paths to the
   native libraries in the .classpath file

Running it:
7. Choose a configuration file and a source code file
   Example: test/config/explicitAnalysisInf.properties
            test/tests/single/loop1.c
   Check that the configuration file does not contain any non-existent paths 
8. Running it from Eclipse:
   Create a run configuration with main class "cmdline.CPAMain", 
   program arguments "-config <CONFIG_FILE> <SOURCE_FILE>", and
   VM arguments "-Djava.library.path=lib/native/<ENVIRONMENT>" 
   specifying your environment for the library path. 
   Settings for <ENVIRONMENT>: 
   		ppc-macosx, x86_64-linux, x86-linux, x86-macosx,  x86-win32

Or:
8. Running it from command line:
   Execute "test/scripts/simple/cpa.sh -config <CONFIG_FILE> <SOURCE_FILE>"
   You need to edit this script, if your Eclipse is not in ~/eclipse,
   /opt/eclipse or ~/Desktop/eclipse


Troubleshooting:
- Imports starting with org.eclipse are not recognized.
  Solution: Double-check PDE is installed (see step 2).
  			This renders manually inserting the respective .jar files unnecessary.

 
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


CONFIGURATION OPTIONS:
Name				possible values		default		recommended	explanation			
-------------------------------------------------------------------------------------------------------------------------------------
log.path			path+filename		<ERROR>		CPALog.txt	name of logfile
log.level			0-1000, on, off, ...	<ERROR>		<DEPENDS>	verbosity level (higher is less verbose)
dot.export			true/false		false		<DEPENDS>	write CFA to file
dot.path			path			"."		<DEPENDS>	CFA graph output file
reachedPath.export		true/false		false		false		write ART to file (needs cpa.useART)
reachedPath.file		path+filename				<DEPENDS>	ART graph output file
cfa.simplify			true/false		false		true		run simplifications below
cfa.check			true/false		false		true		run consistency checks on CFA
cfa.combineBlockStatements	true/false		false		false		combine statements and declarations into MultiStatement and MultiDeclaration edges respectively
cfa.removeIrrelevantForErrorLocations	true/false	false		true		remove all paths not leading to error location
cfa.removeDeclarations		true/false		false		false		remove all DeclarationEdges from CFA
parser.dialect			C99, GNUC		C99		GNUC		C dialect which Eclipse CDT parser uses
analysis.interprocedural	true/false		false		true		combine function CFAs to global CFA
analysis.useRefinement		true/false		false		<DEPENDS>	use refinement algorithm (needs cpa.useART)
anaylsis.useART			true/false		false		false		wrap CPA in ART CPA
analysis.useCBMC		true/false		false		false		use CBMC to check counter examples (currently only if analysis.useRefinement)
analysis.useSummaryLocations	true/false		false		false		use summary edges for loop-free parts of the program		
analysis.useBlockEdges		true/false		false		false		use block edges for branch-free parts of the program (not if useSummaryLocations)
analysis.useGlobalVars		true/false		false		true		include global variable declarations (not if useSummaryLocations or useBlockEdges)
analysis.noExternalCalls	true/false		false		true		don't create call/return edges for external function calls
analysis.noCompositeCPA		true/false		false		true		don't use composite CPA if there is only one CPA
predicates.path
analysis.entryFunction		function name		<ERROR>		main		name of function where analysis should start
analysis.bfs			true/false		false		false		use BFS or DFS as strategy for visiting states
analysis.programs		path							for running CPAchecker as eclipse plugin
analysis.cpas			list of class names	<ERROR>		<DEPENDS>	comma-separated list of CPAs to use
analysis.mergeOperators		list of Strings				<DEPENDS>	comma-separated list of Strings passed to the above CPAs as name of the merge operator
analysis.stopOperators		list of Strings				<DEPENDS>	comma-separated list of Strings passed to the above CPAs as name of the stop operator
analysis.dontPrintReachableStates true/false		false		true		don't print set of reached abstract states after analysis
analysis.topsort
analysis.useGlobalVars		true/false		false		true		
analysis.queryDrivenProgramTesting	true/false	false		false		use QueryDrivenProgramTesting instead of normal algorithm
analysis.useFunctionDeclarations	true/false	false		true		include declarations of external functions in CFA
analysis.programNames		path+filename		<CMDLINE>	<CMDLINE>	the file to analyze