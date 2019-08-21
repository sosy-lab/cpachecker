# 1 "SynchronousSimulator.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "SynchronousSimulator.c"
# 14 "SynchronousSimulator.c"
# 1 "/home/cheolgi/blast/test/headers/assert.h" 1
# 36 "/home/cheolgi/blast/test/headers/assert.h"
# 1 "/usr/include/features.h" 1 3 4
# 313 "/usr/include/features.h" 3 4
# 1 "/usr/include/bits/predefs.h" 1 3 4
# 314 "/usr/include/features.h" 2 3 4
# 346 "/usr/include/features.h" 3 4
# 1 "/usr/include/sys/cdefs.h" 1 3 4
# 353 "/usr/include/sys/cdefs.h" 3 4
# 1 "/usr/include/bits/wordsize.h" 1 3 4
# 354 "/usr/include/sys/cdefs.h" 2 3 4
# 347 "/usr/include/features.h" 2 3 4
# 378 "/usr/include/features.h" 3 4
# 1 "/usr/include/gnu/stubs.h" 1 3 4



# 1 "/usr/include/bits/wordsize.h" 1 3 4
# 5 "/usr/include/gnu/stubs.h" 2 3 4


# 1 "/usr/include/gnu/stubs-32.h" 1 3 4
# 8 "/usr/include/gnu/stubs.h" 2 3 4
# 379 "/usr/include/features.h" 2 3 4
# 37 "/home/cheolgi/blast/test/headers/assert.h" 2
# 65 "/home/cheolgi/blast/test/headers/assert.h"



void __blast_assert() __attribute__ ((__noreturn__)) {
ERROR: goto ERROR;
}



void __assert_fail (__const char *__assertion, __const char *__file,
      unsigned int __line, __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__)) {
   __blast_assert();
}


void __assert_perror_fail (int __errnum, __const char *__file,
      unsigned int __line,
      __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__)) {
   __blast_assert();
}




void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__)) {
 __blast_assert();
}




# 15 "SynchronousSimulator.c" 2







struct SharedVars {
 char side1;
 char side2;
};

int main(int argc, char** argv)
{
  struct SharedVars sharedVars_0;
  struct SharedVars sharedVars_1;

  int __BLAST_NONDET;
  char side1;
  char side2;





  char side1Failed = 1;
  char side2Failed = 1;
  char manualSelection;


  sharedVars_0.side1 = -1;
  sharedVars_0.side2 = -1;

  sharedVars_1.side1 = -1;
  sharedVars_1.side2 = -1;

  while(1) {
    side1Failed = __BLAST_NONDET;
    side2Failed = __BLAST_NONDET;
    manualSelection = __BLAST_NONDET;

    side1 = sharedVars_0.side1;
    side2 = sharedVars_0.side2;

 if(!side1Failed) {
   if(side1 == -1 && side2 == -1) {
     sharedVars_1.side1 = 1;
   } else if (side1 == -1) {
     sharedVars_1.side1 = 0;
   } else if (side2 == -1) {
     sharedVars_1.side1 = 1;
   } else if(manualSelection == 1) {
  if(side1 == 0) {
   sharedVars_1.side1 = 1;
  } else {
   sharedVars_1.side1 = 0;
  }
   } else {
     sharedVars_1.side1 = side1;
   }
 } else sharedVars_1.side1 = -1;

 if(!side2Failed) {
   if (side2 == -1) {
     sharedVars_1.side2 = 0;
   } else if (side1 == -1) {
     sharedVars_1.side2 = 1;
   } else if(manualSelection == 1) {
     if(side2 == 0) {
       sharedVars_1.side2 = 1;
     } else {
       sharedVars_1.side2 = 0;
     }
   } else sharedVars_1.side2 = side2;
 } else sharedVars_1.side2 = -1;

 sharedVars_0.side1 = sharedVars_1.side1;
 sharedVars_0.side2 = sharedVars_1.side2;

 if(sharedVars_0.side1 == -1 && sharedVars_0.side2 == 0)
 ((void) ((0) ? 0 : (__assert_fail ("0", "SynchronousSimulator.c", 95, __PRETTY_FUNCTION__), 0)));

 sharedVars_1.side1 = -1;
 sharedVars_1.side2 = -1;
    }

    return 0;
}
