# 1 "va_list1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "va_list1/main.c"
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



# 2 "va_list1/main.c" 2



int my_f(int x, ...)
{
  __builtin_va_list list;
  __builtin_va_start(list, x);

  int value;
  unsigned i;

  for(i=0; i<x; i++)
    value=__builtin_va_arg(list, int);

  __builtin_va_end(list);

  return value;
}

int my_h(__builtin_va_list list)
{
  __builtin_va_arg(list, int);
  return __builtin_va_arg(list, int);
}

int my_g(int x, ...)
{
  int result;
  __builtin_va_list list;
  __builtin_va_start(list, x);
  result=my_h(list);
  __builtin_va_end(list);
  return result;
}

int main()
{
  ((my_f(3, 10, 20, 30)==30) ? (void) (0) : __assert_fail ("my_f(3, 10, 20, 30)==30", "va_list1/main.c", 39, __PRETTY_FUNCTION__));
  ((my_f(1, 10, 20, 30)==10) ? (void) (0) : __assert_fail ("my_f(1, 10, 20, 30)==10", "va_list1/main.c", 40, __PRETTY_FUNCTION__));
  ((my_f(1, 10)==10) ? (void) (0) : __assert_fail ("my_f(1, 10)==10", "va_list1/main.c", 41, __PRETTY_FUNCTION__));
  ((my_g(11, 22, 33)==33) ? (void) (0) : __assert_fail ("my_g(11, 22, 33)==33", "va_list1/main.c", 42, __PRETTY_FUNCTION__));
}
