Some information about the compiliation of the Apron domain:
- Can be found here: https://antoinemine.github.io/Apron/doc/
- Compile for Java (excpluding everything not needed) with: 
  ./configure --no-cxx --no-ocaml --no-ppl --no-ocaml-plugins --no-ocamlfind
  make
- Then you can find the Java JNI files in the japron folder.
- Several shared libraries from across the Apron folders are needed for CPAchecker. Test by using ldd on the shared libraries, or by executing the Java tests in the japron folder with:
  LD_LIBRARY_PATH=. java -ea -cp apron.jar:. apron.Test 
- Copy all necassary shared libs and Java files to the CPAchecker folder with this file.
