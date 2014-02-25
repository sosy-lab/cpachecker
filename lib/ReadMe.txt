Libraries Used by CPAchecker
============================


Binary Libraries
----------------

Binary libraries should be provided via Ivy.

NOTE: If you add a binary library to CPAchecker, be sure to update MANIFEST.MF
with the Eclipse PDE wizard!

All libraries in the directory lib/java/ are managed by Apache Ivy.
To add libraries there, add them to the file ivy.xml.
Do not store any file in that directory, Ivy will delete it!
To generate a report listing all libraries managed by Ivy,
call "ant report-dependencies".

- cbmc: CBMC
  http://www.cprover.org/cbmc/
  Open-source license: license-cbmc.txt
  Bit-precise bounded model checker for C

- libJOct.so: Octagon Abstract Domain Library
  http://www.di.ens.fr/~mine/oct/
  Octagon-Abstract-Domain License: license-octagon.txt
  Used for octagon abstract domain
  Source for wrapper in native/source/octagon-libJOct.so/

- libmathsat5j.so: MathSAT5
  http://mathsat.fbk.eu/
  CPAchecker-specific license: license-libmathsatj.txt
  SMT-solver (used for predicate analysis)
  Source for Java wrapper library in native/source/libmathsatj/

- libz3j.so and libz3.so: Z3
  http://z3.codeplex.com/
  Microsoft Research License Agreement: license-Z3.txt
  SMT-solver (used for predicate analysis)
  Source for Java wrapper library in native/source/libz3j/

- libfoci.so: FOCI
  http://www.kenmcmil.com/foci2/
  All rights reserved by Cadence.
  Not included in CPAchecker, needs to be downloaded manually.
  Library is required by Z3 for interpolation.

- jpl.jar and libjpl.so: SWI-PL
  http://www.swi-prolog.org/packages/jpl/java_api/index.html
  http://www.swi-prolog.org/
  Lesser GNU Public License

- chc_lib: CHC/CLP Generalization Operators Library
  It requires a working SWI-Prolog installation.
  Apache License, Version 2.0