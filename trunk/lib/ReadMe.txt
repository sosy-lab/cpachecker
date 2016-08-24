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

- jsylvan.jar and libsylvan.so: Sylvan
  https://github.com/trolando/jsylvan
  http://fmt.ewi.utwente.nl/tools/sylvan/
  Apache 2.0 License
  BDD package for multi-core CPUs
  Manual for building in native/source/libsylvan.txt

- jpl.jar and libjpl.so: SWI-PL
  http://www.swi-prolog.org/packages/jpl/java_api/index.html
  http://www.swi-prolog.org/
  Lesser GNU Public License

- chc_lib: CHC/CLP Generalization Operators Library
  It requires a working SWI-Prolog installation.
  Apache License, Version 2.0

- symja: Symja is a pure Java library for symbolic mathematics.
  https://bitbucket.org/axelclk/symja_android_library/wiki/Home
  GNU Lesser General Public License
  Symja is needed for simplifying symbolic expressions

