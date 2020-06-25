# 1 "../versisec/NetBSD-libc/progs/NetBSD-libcCVE-2006-6652glob2_anyMeta_int_bad.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/NetBSD-libc/progs/NetBSD-libcCVE-2006-6652glob2_anyMeta_int_bad.c"
# 1 "../versisec/NetBSD-libc/progs/../glob.h" 1
# 1 "../versisec/NetBSD-libc/progs/../lib/stubs.h" 1



# 1 "../versisec/NetBSD-libc/progs/../lib/base.h" 1
# 5 "../versisec/NetBSD-libc/progs/../lib/stubs.h" 2
# 14 "../versisec/NetBSD-libc/progs/../lib/stubs.h"
typedef int size_t;
typedef int bool;





char *strchr(const char *s, int c);
char *strrchr(const char *s, int c);
char *strstr(const char *haystack, const char *needle);
char *strncpy (char *dest, const char *src, size_t n);
char *strncpy_ptr (char *dest, const char *src, size_t n);
char *strcpy (char *dest, const char *src);
unsigned strlen(const char *s);
int strncmp (const char *s1, const char *s2, size_t n);
int strcmp (const char *s1, const char *s2);
char *strcat(char *dest, const char *src);

void *memcpy(void *dest, const void *src, size_t n);

int isascii (int c);
int isspace (int c);

int getc ( );


char *strrand (char *s);
int istrrand (char *s);
int istrchr(const char *s, int c);
int istrrchr(const char *s, int c);
int istrncmp (const char *s1, int start, const char *s2, size_t n);
int istrstr(const char *haystack, const char *needle);



char *r_strncpy (char *dest, const char *src, size_t n);
char *r_strcpy (char *dest, const char *src);
char *r_strcat(char *dest, const char *src);
char *r_strncat(char *dest, const char *src, size_t n);
void *r_memcpy(void *dest, const void *src, size_t n);
# 2 "../versisec/NetBSD-libc/progs/../glob.h" 2
# 44 "../versisec/NetBSD-libc/progs/../glob.h"
typedef int Char;
typedef char u_char;
# 60 "../versisec/NetBSD-libc/progs/../glob.h"
extern int __VERIFIER_nondet_int (void);
# 2 "../versisec/NetBSD-libc/progs/NetBSD-libcCVE-2006-6652glob2_anyMeta_int_bad.c" 2
# 1 "/usr/include/assert.h" 1 3 4
# 37 "/usr/include/assert.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 322 "/usr/include/features.h" 3 4
# 1 "/usr/include/bits/predefs.h" 1 3 4
# 323 "/usr/include/features.h" 2 3 4
# 355 "/usr/include/features.h" 3 4
# 1 "/usr/include/sys/cdefs.h" 1 3 4
# 353 "/usr/include/sys/cdefs.h" 3 4
# 1 "/usr/include/bits/wordsize.h" 1 3 4
# 354 "/usr/include/sys/cdefs.h" 2 3 4
# 356 "/usr/include/features.h" 2 3 4
# 387 "/usr/include/features.h" 3 4
# 1 "/usr/include/gnu/stubs.h" 1 3 4



# 1 "/usr/include/bits/wordsize.h" 1 3 4
# 5 "/usr/include/gnu/stubs.h" 2 3 4




# 1 "/usr/include/gnu/stubs-64.h" 1 3 4
# 10 "/usr/include/gnu/stubs.h" 2 3 4
# 388 "/usr/include/features.h" 2 3 4
# 38 "/usr/include/assert.h" 2 3 4
# 68 "/usr/include/assert.h" 3 4



extern void __assert_fail (__const char *__assertion, __const char *__file,
      unsigned int __line, __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));


extern void __assert_perror_fail (int __errnum, __const char *__file,
      unsigned int __line,
      __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));




extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));



# 3 "../versisec/NetBSD-libc/progs/NetBSD-libcCVE-2006-6652glob2_anyMeta_int_bad.c" 2

int glob2 (Char *pathbuf, Char *pathend, Char *pathlim, Char *pattern)
{
  int i;
  int anymeta;
  Char tmp;

  for (anymeta = 0;;) {




    i = 0;
    while (pattern[i] != 0 && pattern[i] != '/') {
      if ((((pattern[i])&(0x80)) != 0))
        anymeta = 1;
      if (pathend + i >= pathlim)
        return 1;
      tmp = pattern[i];

      ((pathlim >= pathbuf + sizeof(pathbuf)/sizeof(*pathbuf) -1) ? (void) (0) : __assert_fail ("pathlim >= pathbuf + sizeof(pathbuf)/sizeof(*pathbuf) -1", "../versisec/NetBSD-libc/progs/NetBSD-libcCVE-2006-6652glob2_anyMeta_int_bad.c", 23, __PRETTY_FUNCTION__));

      pathend[i] = tmp;
      i++;
    }

    if (nondet_int ())
      return 0;
  }


}

int main ()
{
  Char *buf;
  Char *pattern;
  Char *bound;

  Char A [2 +1];
  Char B [2 +5];

  buf = A;
  pattern = B;

  bound = A + sizeof(A) - 1;

  glob2 (buf, buf, bound, pattern);

  return 0;
}
