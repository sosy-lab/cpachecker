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

- libmathsatj.so: Mathsat4
  http://mathsat4.disi.unitn.it/
  CPAchecker-specific license: license-libmathsatj.txt
  SMT-solver (used for predicate analysis)
  Source for Java wrapper library in native/source/libmathsatj/
