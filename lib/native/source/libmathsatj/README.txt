=======================================================
 Compilation instruction for the MathSAT Java Bindings
=======================================================

:author: Alberto Griggio <alberto.griggio@disi.unitn.it>

* Download MathSAT from http://mathsat4.disi.unitn.it/download.html
  (or checkout from the repository and compile)

* Open compile.sh and edit the environment variables at the beginning of the
  file (only needed if JDK is in non-standard location)

* Run "compile.sh <PATH_TO_MATHSAT>"
  If everything works, you should obtain and libmathsatj.so

* Copy the file in the lib/native/$arch-$platform directory
  so that CPAchecker can find it
