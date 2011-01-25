Info about this folder:

The original files are in "CPAchecker/test/original-sources". 

-----------------------------------------------------------------

Differences between the c-files and the original-source files:

If there was the expression "#ifdef BLAST_AUTO_1" in a file, 
there are now two files,
one file with the "then"-case and one file with the "else"-case.

-----------------------------------------------------------------

CIL was called with:
"cilly <file>.c --out <file>.cil.c --printCilAsIs --domakeCFG"

"cilly" is not "cilly.asm.exe"!
-----------------------------------------------------------------

If there is the expression "int VERDICT_UNSAFE" in a file, 
the file is named "*_BUG.cil.c".