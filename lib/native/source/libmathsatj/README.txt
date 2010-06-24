=======================================================
 Compilation instruction for the MathSAT Java Bindings
=======================================================

:author: Alberto Griggio <alberto.griggio@disi.unitn.it>

* Download MathSAT from http://mathsat4.disi.unitn.it/download.html
  (or checkout from the repository and compile)

* Open compile.sh and edit the environment variables at the beginning of the
  file (only needed if JDK is in non-standard location)

* Run "compile.sh <PATH_TO_MATHSAT>"
  If everything works, you should obtain mathsat.jar and libmathsatj.so 

* Copy the files in the appropriate locations so that CPAChecker can find
  them:
  - For mathsat.jar, this is lib/
  - For libmathsatj.so, this is lib/native/$arch-$platform

