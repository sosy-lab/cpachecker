# 1 "offsetof1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "offsetof1/main.c"
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



# 2 "offsetof1/main.c" 2

struct S
{
  int i;
  char ch;
  int j;

  struct Ssub
  {
    int x, y;
  } sub, array[100];
};

struct S s;

int main(void)
{
  unsigned char ch;



  ((__builtin_offsetof(struct S, i)==0) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, i)==0", "offsetof1/main.c", 23, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, ch)==4) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, ch)==4", "offsetof1/main.c", 24, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, j)==8) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, j)==8", "offsetof1/main.c", 25, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, sub.x)==12) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, sub.x)==12", "offsetof1/main.c", 26, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, sub.y)==16) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, sub.y)==16", "offsetof1/main.c", 27, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, array)==16+4) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, array)==16+4", "offsetof1/main.c", 28, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, array[1])==16+12) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, array[1])==16+12", "offsetof1/main.c", 29, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, array[1].y)==16+12+4) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, array[1].y)==16+12+4", "offsetof1/main.c", 30, __PRETTY_FUNCTION__));
  ((__builtin_offsetof(struct S, array[ch].y)==16+4+4+ch*sizeof(struct Ssub)) ? (void) (0) : __assert_fail ("__builtin_offsetof(struct S, array[ch].y)==16+4+4+ch*sizeof(struct Ssub)", "offsetof1/main.c", 31, __PRETTY_FUNCTION__));





  (((long int)&((struct S *)0)->i==0) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->i==0", "offsetof1/main.c", 37, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->ch==4) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->ch==4", "offsetof1/main.c", 38, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->j==8) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->j==8", "offsetof1/main.c", 39, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->sub.x==12) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->sub.x==12", "offsetof1/main.c", 40, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->sub.y==16) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->sub.y==16", "offsetof1/main.c", 41, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->array==16+4) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->array==16+4", "offsetof1/main.c", 42, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->array[1]==16+12) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->array[1]==16+12", "offsetof1/main.c", 43, __PRETTY_FUNCTION__));
  (((long int)&((struct S *)0)->array[1].y==16+12+4) ? (void) (0) : __assert_fail ("(long int)&((struct S *)0)->array[1].y==16+12+4", "offsetof1/main.c", 44, __PRETTY_FUNCTION__));




  enum { E1 = __builtin_offsetof(struct S, ch) };


  enum { E2 = (long int)&((struct S *)0)->ch };
  enum { E3 = (long int)&((struct S *)0)->array[1].y };

}
