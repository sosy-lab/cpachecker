# 1 "Struct_Initialization6/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Initialization6/main.c"
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



# 2 "Struct_Initialization6/main.c" 2

struct X
{
  struct Y
  {
    int z;
  } f [3];

  int g, h;

} foo = { .g=200, .f[1].z=100 };

struct Z {
  int a1, a2, a3, a4, a5;
} z = { 10, .a3=30, 40 };

static int enable[32] = { 1, [30] = 2, 3 };

int main()
{
  ((foo.f[0].z==0) ? (void) (0) : __assert_fail ("foo.f[0].z==0", "Struct_Initialization6/main.c", 22, __PRETTY_FUNCTION__));
  ((foo.f[1].z==100) ? (void) (0) : __assert_fail ("foo.f[1].z==100", "Struct_Initialization6/main.c", 23, __PRETTY_FUNCTION__));
  ((foo.f[2].z==0) ? (void) (0) : __assert_fail ("foo.f[2].z==0", "Struct_Initialization6/main.c", 24, __PRETTY_FUNCTION__));
  ((foo.g==200) ? (void) (0) : __assert_fail ("foo.g==200", "Struct_Initialization6/main.c", 25, __PRETTY_FUNCTION__));
  ((foo.h==0) ? (void) (0) : __assert_fail ("foo.h==0", "Struct_Initialization6/main.c", 26, __PRETTY_FUNCTION__));

  ((z.a1==10) ? (void) (0) : __assert_fail ("z.a1==10", "Struct_Initialization6/main.c", 28, __PRETTY_FUNCTION__));
  ((z.a2==0) ? (void) (0) : __assert_fail ("z.a2==0", "Struct_Initialization6/main.c", 29, __PRETTY_FUNCTION__));
  ((z.a3==30) ? (void) (0) : __assert_fail ("z.a3==30", "Struct_Initialization6/main.c", 30, __PRETTY_FUNCTION__));
  ((z.a4==40) ? (void) (0) : __assert_fail ("z.a4==40", "Struct_Initialization6/main.c", 31, __PRETTY_FUNCTION__));
  ((z.a5==0) ? (void) (0) : __assert_fail ("z.a5==0", "Struct_Initialization6/main.c", 32, __PRETTY_FUNCTION__));

  ((enable[0]==1) ? (void) (0) : __assert_fail ("enable[0]==1", "Struct_Initialization6/main.c", 34, __PRETTY_FUNCTION__));
  ((enable[30]==2) ? (void) (0) : __assert_fail ("enable[30]==2", "Struct_Initialization6/main.c", 35, __PRETTY_FUNCTION__));
  ((enable[31]==3) ? (void) (0) : __assert_fail ("enable[31]==3", "Struct_Initialization6/main.c", 36, __PRETTY_FUNCTION__));
}
