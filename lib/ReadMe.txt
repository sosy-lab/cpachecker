Libraries contained in this project:
------------------------------------
NOTE: If you add a library to CPAchecker, be sure to update MANIFEST.MF
and build.properties with the Eclipse PDE wizard!

- cbmc: CBMC
  http://www.cprover.org/cbmc/
  Open source license: license-cbmc.txt
  Bit-precise bounded model checker for C

- libcudd.so: CUDD: CU Decision Diagram Package
  http://vlsi.colorado.edu/~fabio/CUDD/
  Open source library: license-libcudd.txt
  BDD library for predicate abstraction.

- csisat: CSIsat
  http://www.sosy-lab.org/~dbeyer/CSIsat/
  Apache License 2.0: ../License_Apache-2.0.txt
  SMT solver for predicate analysis.

- eclipse/junit.jar: JUnit
  http://junit.sourceforge.net/
  Common Public License 1.0: http://junit.sourceforge.net/cpl-v10.html
  Used for unit tests.

- eclipse/org.eclipse.*: Eclipse and Eclipse CDT
  http://www.eclipse.org/ and http://www.eclipse.org/cdt/
  Eclipse Public License 1.0: http://www.eclipse.org/org/documents/epl-v10.php
  Used for parsing C code.

- guava-*.jar: Google Core Libraries for Java
  http://guava-libraries.googlecode.com
  Apache License 2.0: ../License_Apache-2.0.txt
  Contains a lot of helpful data structures.

- icu4j-*.jar: International Components for Unicode
  http://site.icu-project.org/
  ICU License - ICU 1.8.1 and later: license-libicu4j.html
  Needed by Eclipse CDT parser for error messages.

- javabdd-*.jar: JavaBDD
  http://javabdd.sourceforge.net/
  GNU LGPL: GPL.txt
  Java BDD library for predicate abstraction (uses CUDD).

- java-cup-*.jar: CUP LALR Parser Generator for Java
  http://www2.cs.tum.edu/projects/cup/
  CUP Parser Generator License: license-cup.txt
  Used for generating automaton and FQL parsers.

- JFLex.jar: JFlex Scanner Generator for Java
  http://www.jflex.de/
  GNU GPL: GPL.txt
  Used for generating automaton and FQL scanners.
  The generated code is not under GPL.

- jgrapht-*.jar: JGraphT
  http://www.jgrapht.org/
  GNU LGPL: LGPL.txt
  Used for fllesh graphs.

- libJOct.so: Octagon Abstract Domain Library
  http://www.di.ens.fr/~mine/oct/
  Octagon Abstract Domain License: license-octagon.txt
  Used for octagon abstract domain.
  Source for wrapper in native/source/octagon-libJOct.so/

- json_simple-*.jar: JSON.simple -A simple Java toolkit for JSON
  http://code.google.com/p/json-simple/
  Apache License 2.0: ../License_Apache-2.0.txt
  Used for dumping verification result as JSON for report builder.

- mathsat.jar, libmathsatj.so: Mathsat4
  http://mathsat4.disi.unitn.it/
  CPAchecker-specific license: license-libmathsatj.txt
  SMT-solver for predicate analysis.
  Source for Java wrapper library in native/source/libmathsatj/

- libyices.so: Yices SMT solver
  http://yices.csl.sri.com/
  Binary only license: http://yices.csl.sri.com/yices-newlicense.shtml
  SMT-solver for predicate analysis.

- yicesapijava.jar: Yices Java Lite API
  http://atlantis.seidenberg.pace.edu/wiki/lep/Yices%20Java%20API%20Lite
  Open source (license not specified)
  Java wrapper library for Yices.
