# 1 "Sideeffects3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Sideeffects3/main.c"
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



# 2 "Sideeffects3/main.c" 2

int my_f(int arg)
{
  return arg;
}

int x, y, z;

int main()
{







  x=1;
  int array[++x];
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 21, __PRETTY_FUNCTION__));
  ((sizeof(array)==sizeof(int)*2) ? (void) (0) : __assert_fail ("sizeof(array)==sizeof(int)*2", "Sideeffects3/main.c", 22, __PRETTY_FUNCTION__));


  x=1;
  typedef int array_type[++x];
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 27, __PRETTY_FUNCTION__));
  ((sizeof(array_type)==sizeof(int)*2) ? (void) (0) : __assert_fail ("sizeof(array_type)==sizeof(int)*2", "Sideeffects3/main.c", 28, __PRETTY_FUNCTION__));


  x=1;
  int local=++x, local2=x;
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 33, __PRETTY_FUNCTION__));
  ((local==2) ? (void) (0) : __assert_fail ("local==2", "Sideeffects3/main.c", 34, __PRETTY_FUNCTION__));
  ((local2==2) ? (void) (0) : __assert_fail ("local2==2", "Sideeffects3/main.c", 35, __PRETTY_FUNCTION__));


  x=1;
  int return_value=my_f(++x);
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 40, __PRETTY_FUNCTION__));
  ((return_value==2) ? (void) (0) : __assert_fail ("return_value==2", "Sideeffects3/main.c", 41, __PRETTY_FUNCTION__));


  x=1;
  int *p=&x;
  y=++(*p);
  ((y==2) ? (void) (0) : __assert_fail ("y==2", "Sideeffects3/main.c", 47, __PRETTY_FUNCTION__));
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 48, __PRETTY_FUNCTION__));


  x=1;
  struct struct_type
  {
    int a[++x];
    int b;
  };
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 57, __PRETTY_FUNCTION__));
  ((sizeof(struct struct_type)==sizeof(int)*2+sizeof(int)) ? (void) (0) : __assert_fail ("sizeof(struct struct_type)==sizeof(int)*2+sizeof(int)", "Sideeffects3/main.c", 58, __PRETTY_FUNCTION__));


  x++;
  ((sizeof(struct struct_type)==sizeof(int)*2+sizeof(int)) ? (void) (0) : __assert_fail ("sizeof(struct struct_type)==sizeof(int)*2+sizeof(int)", "Sideeffects3/main.c", 62, __PRETTY_FUNCTION__));


  x=1;
  y=1;
  struct other_struct
  {
    int a[++x];
  } v1[y++], v2[y++], v3[y++];
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 71, __PRETTY_FUNCTION__));
  ((y==4) ? (void) (0) : __assert_fail ("y==4", "Sideeffects3/main.c", 72, __PRETTY_FUNCTION__));
  ((sizeof(v1)==sizeof(int)*2*1) ? (void) (0) : __assert_fail ("sizeof(v1)==sizeof(int)*2*1", "Sideeffects3/main.c", 73, __PRETTY_FUNCTION__));
  ((sizeof(v2)==sizeof(int)*2*2) ? (void) (0) : __assert_fail ("sizeof(v2)==sizeof(int)*2*2", "Sideeffects3/main.c", 74, __PRETTY_FUNCTION__));
  ((sizeof(v3)==sizeof(int)*2*3) ? (void) (0) : __assert_fail ("sizeof(v3)==sizeof(int)*2*3", "Sideeffects3/main.c", 75, __PRETTY_FUNCTION__));


  x=1;
  (struct { int a[x++]; } *)0;
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 80, __PRETTY_FUNCTION__));


  x=1;
  (int (*)(int a[x++]))0;
  ((x==1) ? (void) (0) : __assert_fail ("x==1", "Sideeffects3/main.c", 85, __PRETTY_FUNCTION__));


  x=1;
  ((sizeof(struct { int a[x++]; })==sizeof(int)) ? (void) (0) : __assert_fail ("sizeof(struct { int a[x++]; })==sizeof(int)", "Sideeffects3/main.c", 89, __PRETTY_FUNCTION__));
  ((x==2) ? (void) (0) : __assert_fail ("x==2", "Sideeffects3/main.c", 90, __PRETTY_FUNCTION__));


  x=y=1;
  typedef int my_array1[x][y];
  x++;
  ((sizeof(my_array1)==sizeof(int)) ? (void) (0) : __assert_fail ("sizeof(my_array1)==sizeof(int)", "Sideeffects3/main.c", 96, __PRETTY_FUNCTION__));


}
