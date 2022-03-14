# 1 "String1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "String1/main.c"
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



# 2 "String1/main.c" 2

char s[]="abc\001";

char *p="abc";

int input;

int main()
{
  ((s[1]=='b') ? (void) (0) : __assert_fail ("s[1]=='b'", "String1/main.c", 11, __PRETTY_FUNCTION__));
  ((s[4]==0) ? (void) (0) : __assert_fail ("s[4]==0", "String1/main.c", 12, __PRETTY_FUNCTION__));


  s[0]='x';

  ((p[2]=='c') ? (void) (0) : __assert_fail ("p[2]=='c'", "String1/main.c", 17, __PRETTY_FUNCTION__));

  p=s;


  p[1]='y';

  ((s[1]=='y') ? (void) (0) : __assert_fail ("s[1]=='y'", "String1/main.c", 24, __PRETTY_FUNCTION__));

  {
    const char local_string[]="asd123";

    ((local_string[0]=='a') ? (void) (0) : __assert_fail ("local_string[0]=='a'", "String1/main.c", 29, __PRETTY_FUNCTION__));
    ((sizeof(local_string)==7) ? (void) (0) : __assert_fail ("sizeof(local_string)==7", "String1/main.c", 30, __PRETTY_FUNCTION__));
    ((local_string[6]==0) ? (void) (0) : __assert_fail ("local_string[6]==0", "String1/main.c", 31, __PRETTY_FUNCTION__));
  }






  typedef __typeof__(L'X') wide_char_type;


  unsigned width=sizeof(wide_char_type);




  ((width==4) ? (void) (0) : __assert_fail ("width==4", "String1/main.c", 47, __PRETTY_FUNCTION__));


  ((sizeof(L"12" "34")==5*width) ? (void) (0) : __assert_fail ("sizeof(L\"12\" \"34\")==5*width", "String1/main.c", 50, __PRETTY_FUNCTION__));
  ((sizeof("12" L"34")==5*width) ? (void) (0) : __assert_fail ("sizeof(\"12\" L\"34\")==5*width", "String1/main.c", 51, __PRETTY_FUNCTION__));

  wide_char_type wide[]=L"1234\x0fff";
  ((sizeof(wide)==6*width) ? (void) (0) : __assert_fail ("sizeof(wide)==6*width", "String1/main.c", 54, __PRETTY_FUNCTION__));
  ((wide[4]==0x0fff) ? (void) (0) : __assert_fail ("wide[4]==0x0fff", "String1/main.c", 55, __PRETTY_FUNCTION__));
}
