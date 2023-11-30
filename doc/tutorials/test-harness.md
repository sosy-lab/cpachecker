<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# CPAchecker Test Harness

When CPAchecker finds a property violation, it tries to generate a test harness
that can reproduce the found violation through test execution.

We consider this example program:

```c
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function);

extern int __VERIFIER_nondet_int(void);

// Checks whether there are 5 or more non-prime numbers between 2 and 100.
int main() {
  int nonprimesCount = 0;
  int lastN = 2;
  while (1) {
    // Choose a number n, greater than the last number and <= 100.
    int choiceN = __VERIFIER_nondet_int();
    if (choiceN <= lastN || choiceN > 100) {
      break;
    }
    lastN = choiceN;
    // Choose a number div, >= 2 and smaller than the choiceN.
    int choiceDiv = __VERIFIER_nondet_int();
    if (choiceDiv < 2 || choiceDiv >= choiceN) {
      break;
    }
    // If 'n' is divisible by 'div', it is not a prime.
    // Then increase the non-prime counter by one.
    if (choiceN % choiceDiv == 0) {
      nonprimesCount++;
    }
  }

  // If we found at least 5 non-primes, fail with an assertion.
  if (nonprimesCount >= 5) {
    __assert_fail("There are 5 or more non-prime numbers between 2 and 100", "example_bug.c", 39, "main");
  }
}
```

Method `__VERIFIER_nondet_int` is a special method that tells CPAchecker
that on every call, a new, arbitrary int value is returned.  
The program checks whether there are at least 5 non-prime numbers between
2 and 100.
It does so with a non-deterministic procedure:
First, the program gets an arbitrary value 'choiceN'.
This value has to be between the last choiceN (initially: 2)
and 100. Otherwise, the procedure immediately goes to the check
whether 5 or more non-prime numbers have been found.
Then, the program gets an arbitrary value 'choiceDiv'.
This value has to be between 2 and 'choiceN'.
If 'choiceN' is divisable by 'choiceDiv',
the program has found a non-prime number (choiceN).
In the end, the program checks whether 5 or more non-prime numbers were found.
If this is true, the program has a failing assertion.

1. Run CPAchecker on this program, with its default analysis:
    ```
    scripts/cpa.sh -default doc/examples/example_bug.c
    ```
    Expected output:
    ```
    Running CPAchecker with default heap size (1200M). Specify a larger value with -heap if you have more RAM.
    Running CPAchecker with default stack size (1024k). Specify a larger value with -stack if needed.
    Language C detected and set for analysis (CPAMain.detectFrontendLanguageIfNecessary, INFO)
    
    Using the following resource limits: CPU-time limit of 900s (ResourceLimitChecker.fromConfiguration, INFO)
    [.. snip output ..]
    Stopping analysis ... (CPAchecker.runAlgorithm, INFO)
    
    Verification result: FALSE. Property violation (assertion in line 39: Condition "There are 5 or more non-prime numbers between 2 and 100" failed in "example_bug.c", line 39) found by chosen configuration.
    More details about the verification run can be found in the directory "./output".
    Graphical representation included in the file "./output/Counterexample.1.html".
    ```
    By default, CPAchecker checks for failing assertions.
    Line "Verification result: FALSE" tells us that CPAchecker figured out that
    the failing assertion in line 39 is reachable.
    It also produces a test harness `output/Counterexample.1.harness.c`
    (the number '1' in the file name may differ for other programs):
    ```
    struct _IO_FILE;
    typedef struct _IO_FILE FILE;
    extern struct _IO_FILE *stderr;
    extern int fprintf(FILE *__restrict __stream, const char *__restrict __format, ...);
    extern void exit(int __status) __attribute__ ((__noreturn__));
    void __assert_fail(const char *__assertion, const char *__file, unsigned int __line, const char *__function) {
      fprintf(stderr, "CPAchecker test harness: property violation reached\n");
      exit(107);
    }
    int __VERIFIER_nondet_int() {
      static unsigned int test_vector_index = 0;
      int retval;
      switch (test_vector_index) {
        case 0: retval = 6; break;
        case 1: retval = 3; break;
        case 2: retval = 9; break;
        case 3: retval = 3; break;
        case 4: retval = 10; break;
        case 5: retval = 2; break;
        case 6: retval = 14; break;
        case 7: retval = 7; break;
        case 8: retval = 15; break;
        case 9: retval = 3; break;
        case 10: retval = 0; break;
      }
      ++test_vector_index;
      return retval;
    }
    ```
    The test harness consists of two parts:
    1. A definition for the failing method `void __assert_fail(..)`.
       If the property violation is not because a certain error-method like __assert_fail was reached,
       there will be no such definition. In this case, the property violation must be observed by the user's own means.
       Examples for this are labels `ERROR:` or certain return values.
    2. A definition for the __VERIFIER_nondet_int method.
        This defines return values for __VERIFIER_nondet_int
        that reach the failing assertion.
        In our case, this is alternating numbers for 'choiceN' (6, 9, 10, 14, 15, 0)
        and for 'choiceDiv' (3, 3, 2, 7, 3).
        The last 'choiceN', 0, makes the program break the while-loop.

2. Compile the test harness against the example program:
    ```
    gcc output/Counterexample.1.harness.c doc/examples/example_bug.c -o ./testviolation
    ```
    This produces executable `./testviolation`.

3. Execute `./testviolation`:
    ```
    ./testviolation
    ```
    Expected output:
    ```
    CPAchecker test harness: property violation reached
    ```
    
    This message tells you that the execution reached the failing assertion.
