Libraries contained in this project:
------------------------------------
NOTE: If you add a library to CPAchecker, be sure to update MANIFEST.MF
with the Eclipse PDE wizard!

All libraries in the lib/java directory are managed by Apache Ivy.
To add libraries there, add them to the ivy.xml file.
Do not put any files in that directory, Ivy will delete them!
To generate a report listing all the libraries managed by Ivy,
call "ant report-dependencies". 

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

- libJOct.so: Octagon Abstract Domain Library
  http://www.di.ens.fr/~mine/oct/
  Octagon Abstract Domain License: license-octagon.txt
  Used for octagon abstract domain.
  Source for wrapper in native/source/octagon-libJOct.so/

- libmathsatj.so: Mathsat4
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
