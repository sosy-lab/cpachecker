# 1 "Bool1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Bool1/main.c"
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



# 2 "Bool1/main.c" 2

int main() {
  _Bool b1, b2, b3;

  b1=0;
  b1++;
  ((b1) ? (void) (0) : __assert_fail ("b1", "Bool1/main.c", 8, __PRETTY_FUNCTION__));

  b2=1;
  b2+=10;
  ((b2) ? (void) (0) : __assert_fail ("b2", "Bool1/main.c", 12, __PRETTY_FUNCTION__));

  b3=b1+b2;
  ((b3==1) ? (void) (0) : __assert_fail ("b3==1", "Bool1/main.c", 15, __PRETTY_FUNCTION__));


  struct
  {
    _Bool f1, f2, f3;
    _Bool f4: 1, f5: 1;
  } s;

  ((sizeof(s)==4) ? (void) (0) : __assert_fail ("sizeof(s)==4", "Bool1/main.c", 24, __PRETTY_FUNCTION__));

  s.f1=2;
  ((s.f1==1) ? (void) (0) : __assert_fail ("s.f1==1", "Bool1/main.c", 27, __PRETTY_FUNCTION__));

  s.f4=1;
  ((s.f4) ? (void) (0) : __assert_fail ("s.f4", "Bool1/main.c", 30, __PRETTY_FUNCTION__));

  *((unsigned char *)(&s.f2))=1;
  ((s.f2) ? (void) (0) : __assert_fail ("s.f2", "Bool1/main.c", 33, __PRETTY_FUNCTION__));
}
