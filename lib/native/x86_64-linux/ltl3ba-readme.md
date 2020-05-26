Instructions for building a new `ltl3ba` binary
===============================================

Requirements
----------------------------

LTL3BA needs the [BuDDy (Binary Decision Diagram) library](http://sourceforge.net/projects/buddy/).
LTL3BA was created and tested with BuDDy version 2.4.

Steps for building BuDDy
----------------------------
The url is taken from the sourceforge repository mentioned above.
```
wget https://sourceforge.net/projects/buddy/files/latest/download -O buddy-2.4.tar.gz
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
wget https://sourceforge.net/projects/ltl3ba/files/latest/download -O ltl3ba-1.1.3.tar.gz
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
