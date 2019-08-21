# 1 "Struct_Padding1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Padding1/main.c"
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



# 2 "Struct_Padding1/main.c" 2




struct my_struct1
{
  int i;
  char ch;

  struct
  {

    int j;
  };



  unsigned bf1:1;
  unsigned bf2:28;
} xx1 =
{
  1, 2, { .j=3 }
};

struct my_struct2
{
  int i;
  char ch[4];


  int j;

  char ch2;

} xx2;

struct my_struct3 {
   unsigned int bit_field : 1;
   int i;
} xx3= { 1, 2 };

int some_array__LINE__[(sizeof(xx1)==4+1+3+4+4) ? 1 : -1];
int some_array__LINE__[(sizeof(xx2)==4+4+4+4) ? 1 : -1];

int main()
{
  ((xx1.i==1) ? (void) (0) : __assert_fail ("xx1.i==1", "Struct_Padding1/main.c", 48, __PRETTY_FUNCTION__));
  ((xx1.ch==2) ? (void) (0) : __assert_fail ("xx1.ch==2", "Struct_Padding1/main.c", 49, __PRETTY_FUNCTION__));
  ((xx1.j==3) ? (void) (0) : __assert_fail ("xx1.j==3", "Struct_Padding1/main.c", 50, __PRETTY_FUNCTION__));


  char *p=&xx1.ch;
  ((p[0]==2) ? (void) (0) : __assert_fail ("p[0]==2", "Struct_Padding1/main.c", 54, __PRETTY_FUNCTION__));
  ((p[1]==0) ? (void) (0) : __assert_fail ("p[1]==0", "Struct_Padding1/main.c", 55, __PRETTY_FUNCTION__));
  ((p[2]==0) ? (void) (0) : __assert_fail ("p[2]==0", "Struct_Padding1/main.c", 56, __PRETTY_FUNCTION__));
  ((p[3]==0) ? (void) (0) : __assert_fail ("p[3]==0", "Struct_Padding1/main.c", 57, __PRETTY_FUNCTION__));

  ((xx3.bit_field==1) ? (void) (0) : __assert_fail ("xx3.bit_field==1", "Struct_Padding1/main.c", 59, __PRETTY_FUNCTION__));
  ((xx3.i==2) ? (void) (0) : __assert_fail ("xx3.i==2", "Struct_Padding1/main.c", 60, __PRETTY_FUNCTION__));
}
