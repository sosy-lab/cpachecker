# CPAchecker Test Harness

When CPAchecker finds a property violation, it tries to generate a test harness
that can reproduce the found violation through test execution.

We consider this example program:

```c
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

extern int __VERIFIER_nondet_int(void);
int main(void) {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  if (x > 100 && x < 1000 && y > 0 && y < 200 && x * y == 39203) {
    __assert_fail("x * y != 39203", "example_bug_with_nondet.c", 10, __extension__ __PRETTY_FUNCTION__);
  }
}
```

Method `__VERIFIER_nondet_int` is a special method that tells CPAchecker
that on every call, a new, arbitrary int value is returned.  
The program receives two arbitrary int values x and y.
It then checks whether the constraints 100 < x < 1000, 0 < y < 200,
and x * y == 39203 are true.
If they are, the program has a failing assertion.

1. Run CPAchecker on this program, with its default analysis:
    ```
    scripts/cpa.sh -default doc/examples/example_bug_with_nondet.c
    ```
    Expected output:
    ```
    Running CPAchecker with default heap size (1200M). Specify a larger value with -heap if you have more RAM.
    Running CPAchecker with default stack size (1024k). Specify a larger value with -stack if needed.
    Language C detected and set for analysis (CPAMain.detectFrontendLanguageIfNecessary, INFO)
    
    Using the following resource limits: CPU-time limit of 900s (ResourceLimitChecker.fromConfiguration, INFO)
    [.. snip output ..]
    Stopping analysis ... (CPAchecker.runAlgorithm, INFO)
    
    Verification result: FALSE. Property violation (assertion in line 10: Condition "x * y != 39203" failed in "example_bug_with_nondet.c", line 10) found by chosen configuration.
    More details about the verification run can be found in the directory "./output".
    Graphical representation included in the file "./output/Counterexample.1.html".
    ```
    By default, CPAchecker checks for failing assertions.
    Line "Verification result: FALSE" tells us that CPAchecker figured out that
    the failing assertion in line 10 is reachable.
    It also produces a test harness `output/Counterexample.1.harness.c`
    (the number '1' may differ for other programs):
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
        case 0: retval = 197; break;
        case 1: retval = 199; break;
      }
      ++test_vector_index;
      return retval;
    }
    ```
    The test harness consists of two parts:
    1. A definition for the failing method `void __assert_fail(..)`, and
    2. A definition for the __VERIFIER_nondet_int method.
        This defines return values for __VERIFIER_nondet_int
        that reach the failing assertion:
        On the first call, __VERIFIER_nondet_int should return 197,
        and on the second call, it should return 199.

2. Compile the test harness against the example program:
    ```
    gcc output/Counterexample.1.harness.c doc/examples/example_bug_with_nondet.c -o ./testviolation
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
