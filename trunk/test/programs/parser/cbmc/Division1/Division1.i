# 1 "Division1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Division1/main.c"
# 1 "/usr/include/assert.h" 1 3 4
# 37 "/usr/include/assert.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 324 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/predefs.h" 1 3 4
# 325 "/usr/include/features.h" 2 3 4
# 357 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 1 3 4
# 378 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 379 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 358 "/usr/include/features.h" 2 3 4
# 389 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 1 3 4



# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 5 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4




# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs-64.h" 1 3 4
# 10 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4
# 390 "/usr/include/features.h" 2 3 4
# 38 "/usr/include/assert.h" 2 3 4
# 68 "/usr/include/assert.h" 3 4



extern void __assert_fail (__const char *__assertion, __const char *__file,
      unsigned int __line, __const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));


extern void __assert_perror_fail (int __errnum, __const char *__file,
      unsigned int __line,
      __const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));




extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));



# 2 "Division1/main.c" 2

int main()
{
  int k1 = 5 / 2;
  ((k1 == 2) ? (void) (0) : __assert_fail ("k1 == 2", "Division1/main.c", 6, __PRETTY_FUNCTION__));

  int k2 = -5 / 2;
  ((k2 == -2) ? (void) (0) : __assert_fail ("k2 == -2", "Division1/main.c", 9, __PRETTY_FUNCTION__));

  int k3 = 5 / -2;
  ((k3 == -2) ? (void) (0) : __assert_fail ("k3 == -2", "Division1/main.c", 12, __PRETTY_FUNCTION__));

  int k4 = -5 / -2;
  ((k4 == 2) ? (void) (0) : __assert_fail ("k4 == 2", "Division1/main.c", 15, __PRETTY_FUNCTION__));
}
