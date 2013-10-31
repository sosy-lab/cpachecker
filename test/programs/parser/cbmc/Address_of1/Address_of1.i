# 1 "Address_of1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Address_of1/main.c"
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



# 2 "Address_of1/main.c" 2

int main()
{
  int some_int=20;
  int *p;
  p=(int []){ 1, 2, 3, some_int };

  ((p[0]==1) ? (void) (0) : __assert_fail ("p[0]==1", "Address_of1/main.c", 9, __PRETTY_FUNCTION__));
  ((p[1]==2) ? (void) (0) : __assert_fail ("p[1]==2", "Address_of1/main.c", 10, __PRETTY_FUNCTION__));
  ((p[2]==3) ? (void) (0) : __assert_fail ("p[2]==3", "Address_of1/main.c", 11, __PRETTY_FUNCTION__));
  ((p[3]==20) ? (void) (0) : __assert_fail ("p[3]==20", "Address_of1/main.c", 12, __PRETTY_FUNCTION__));

  struct S { int x, y; } *q;

  q=&(struct S){ .x=1 };

  ((q->x==1) ? (void) (0) : __assert_fail ("q->x==1", "Address_of1/main.c", 18, __PRETTY_FUNCTION__));
  ((q->y==0) ? (void) (0) : __assert_fail ("q->y==0", "Address_of1/main.c", 19, __PRETTY_FUNCTION__));

  const char *sptr="asd";
  ((sptr[0]=='a') ? (void) (0) : __assert_fail ("sptr[0]=='a'", "Address_of1/main.c", 22, __PRETTY_FUNCTION__));
  ((sptr[1]=='s') ? (void) (0) : __assert_fail ("sptr[1]=='s'", "Address_of1/main.c", 23, __PRETTY_FUNCTION__));
  ((sptr[2]=='d') ? (void) (0) : __assert_fail ("sptr[2]=='d'", "Address_of1/main.c", 24, __PRETTY_FUNCTION__));
  ((sptr[3]==0) ? (void) (0) : __assert_fail ("sptr[3]==0", "Address_of1/main.c", 25, __PRETTY_FUNCTION__));
}
