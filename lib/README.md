<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Libraries Used by CPAchecker
============================

Binary Libraries
----------------

Binary libraries should be provided via Ivy.

All libraries in the directory `lib/java/` are managed by Apache Ivy.
To add libraries there, add them to the file `ivy.xml`.
Do not store any file in that directory, Ivy will delete it!
To generate a report listing all libraries managed by Ivy,
call `ant report-dependencies`.
Licenses that are part of the `runtime` configuration
will be bundled in CPAchecker release archives
and thus their license text and copyright archive needs to be present.
To ensure this add a note about the library to `java-runtime-licenses.txt`.

For all other libraries and tools, document them here.
License and copyright need to be declared in a `.license` file
as supported by the `reuse` tool.

- `apron.jar`, `gmp.jar`, `libjapron.so`, `libjgmp.so`:
  [APRON numerical abstract domain library](http://apron.cri.ensmp.fr/library/)
  and its [Java bindings](https://github.com/tobiatesan/japron)  
  Used for polyhedral and octagon abstract domains

- `cbmc`: [CBMC](http://www.cprover.org/cbmc/)  
  Bit-precise bounded model checker for C

- `libJOct.so`: [Octagon Abstract Domain Library](http://www.di.ens.fr/~mine/oct/)  
  Used for octagon abstract domain  
  Source for wrapper in `native/source/octagon-libJOct.so/`

- `jsylvan.jar` and `libsylvan.so`:
  [Sylvan](http://fmt.ewi.utwente.nl/tools/sylvan/)
  and its [Java bindings](https://github.com/trolando/jsylvan)  
  BDD package for multi-core CPUs  
  Manual for building in `native/source/libsylvan.md`

- `jpl.jar` and `libjpl.so`: [SWI-PL](http://www.swi-prolog.org/)

- `chc_lib`: CHC/CLP Generalization Operators Library  
  It requires a working SWI-Prolog installation.

- `ltl3ba`: [LTL3BA](https://sourceforge.net/projects/ltl3ba/)  
  Translator of LTL formulae to Büchi automata based on LTL2BA
  (command-line binary, includes [BuDDy](https://sourceforge.net/projects/buddy/))

- `z3`: [SMT Solver Z3](https://github.com/Z3Prover/z3)
  The command-line binary is necessary because Ultimate LassoRanker calls it.
