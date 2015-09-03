# 1 "Promotion1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Promotion1/main.c"
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



# 2 "Promotion1/main.c" 2

int main()
{


  unsigned short int a1=1;
  signed int b1=-1;

  if(sizeof(short)<sizeof(int))
    ((a1>b1) ? (void) (0) : __assert_fail ("a1>b1", "Promotion1/main.c", 11, __PRETTY_FUNCTION__));
  else
    ((a1<b1) ? (void) (0) : __assert_fail ("a1<b1", "Promotion1/main.c", 13, __PRETTY_FUNCTION__));



  unsigned char a2=1;
  signed char b2=-1;

  if(sizeof(char)<sizeof(int))
    ((a2>b2) ? (void) (0) : __assert_fail ("a2>b2", "Promotion1/main.c", 21, __PRETTY_FUNCTION__));
  else
    ((a2<b2) ? (void) (0) : __assert_fail ("a2<b2", "Promotion1/main.c", 23, __PRETTY_FUNCTION__));


  unsigned int a3=1;
  signed int b3=-1;
  ((a3<b3) ? (void) (0) : __assert_fail ("a3<b3", "Promotion1/main.c", 28, __PRETTY_FUNCTION__));


  unsigned long long int a4=1;
  long long signed int b4=-1;
  ((a4<b4) ? (void) (0) : __assert_fail ("a4<b4", "Promotion1/main.c", 33, __PRETTY_FUNCTION__));
}
