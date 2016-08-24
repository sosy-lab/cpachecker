# 1 "Linking2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Linking2/main.c"
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



# 2 "Linking2/main.c" 2

# 1 "Linking2/module.h" 1
void f();

struct common_struct
{
  int z;
};

void g(struct common_struct x);
# 4 "Linking2/main.c" 2


typedef char t;


struct my_struct
{
  t t_field;
};


t i=2;


int j=3;

int main()
{
  ((i==2) ? (void) (0) : __assert_fail ("i==2", "Linking2/main.c", 22, __PRETTY_FUNCTION__));
  ((j==3) ? (void) (0) : __assert_fail ("j==3", "Linking2/main.c", 23, __PRETTY_FUNCTION__));

  f();


  ((i==2) ? (void) (0) : __assert_fail ("i==2", "Linking2/main.c", 28, __PRETTY_FUNCTION__));
  ((j==4) ? (void) (0) : __assert_fail ("j==4", "Linking2/main.c", 29, __PRETTY_FUNCTION__));

  struct my_struct xx;
  ((sizeof(xx.t_field)==1) ? (void) (0) : __assert_fail ("sizeof(xx.t_field)==1", "Linking2/main.c", 32, __PRETTY_FUNCTION__));
}

struct struct_tag
{
  short int i;
};
