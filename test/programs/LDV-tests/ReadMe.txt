Info about this folder:

The original files are in "CPAchecker/test/original-sources". 
The modified files are in "c-files".
The CIL-files are in "cil.c-files".

-----------------------------------------------------------------

Differences between the c-files and the original-source files:

The expression "NULL" is replaced by "0".
-> because CIL gets an error: "Cannot resolve variable NULL."

If there was the expression "#ifdef BLAST_AUTO_1" in a file, 
there are now (or in future) two files, 
one file with the "then"-case and one file with the "else"-case.

-----------------------------------------------------------------

CIL was called with:
"cilly.asm.exe <file>.c --out <file>.cil.c --printCilAsIs --domakeCFG"

If there was the expression "int CURRENTLY_UNSAFE" in a file, 
the CIL-file is named "*_BUG.cil.c".