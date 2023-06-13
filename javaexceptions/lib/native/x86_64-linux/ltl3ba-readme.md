<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Instructions for building a new `ltl3ba` binary
===============================================

Note that when updating ltl3ba,
its sources (including statically linked dependencies)
need to be committed to the repo in this directory
to fulfill the GPL requirements!
Also adjust ltl3ba.license where necessary.

Requirements
----------------------------

LTL3BA needs the [BuDDy (Binary Decision Diagram) library](http://sourceforge.net/projects/buddy/).
LTL3BA was created and tested with BuDDy version 2.4.

Steps for building BuDDy
----------------------------
The archive was taken from the sourceforge repository mentioned above
(repacked without doc and examples to save space):
```
tar xf buddy-2.4.tar.xz
cd buddy-2.4
./configure
make
cd ..
```

Steps for building LTL3BA
----------------------------
LTL3BA is open-source and [publicly available](https://sourceforge.net/projects/ltl3ba/).
The current binary in CPAchecker was built using the version 1.1.3.

```
tar xvf ltl3ba.tar.gz
cd ltl3ba-1.1.3
```

Edit Makefile and set paths to BuDDy's files `bdd.h` and `libbdd.a`.
(i.e., edit variables `BUDDY_INCLUDE` and `BUDDY_LIB`)

```
sed -i 's@BUDDY_INCLUDE=/usr/local/include/@BUDDY_INCLUDE=../buddy-2.4/src/@' Makefile
sed -i 's@BUDDY_LIB=/usr/local/lib/@BUDDY_LIB=../buddy-2.4/src/.libs/@' Makefile
```

Edit the respective line in Makefile such that the file `libbdd.a` is included
and linked as static library
(such that it is not required to have it in the library path `LD_LIBRARY_PATH`):
```
sed -i 's@$(CXX) $(CPPFLAGS) -o ltl3ba $(LTL3BA) -L$(BUDDY_LIB) -lbdd@$(CXX) $(CPPFLAGS) -o ltl3ba $(LTL3BA) -Wl,-R $(BUDDY_LIB)libbdd.a -L$(BUDDY_LIB) -lbdd@' Makefile
```

Build the binary (it is going to be named ´ltl3ba`) and test it:
```
make
chmod +x ltl3ba
./ltl3ba -f "G a"
```
