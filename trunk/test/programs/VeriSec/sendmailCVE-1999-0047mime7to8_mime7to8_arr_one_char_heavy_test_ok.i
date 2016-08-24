# 1 "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c"
# 1 "../versisec/sendmail/progs/../lib/stubs.h" 1



# 1 "../versisec/sendmail/progs/../lib/base.h" 1
# 5 "../versisec/sendmail/progs/../lib/stubs.h" 2
# 14 "../versisec/sendmail/progs/../lib/stubs.h"
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
# 2 "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c" 2
# 1 "../versisec/sendmail/progs/../lib/base.h" 1
# 3 "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c" 2
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



# 4 "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c" 2



int main (void)
{
  char fbuf[2 +1];
  int fb;
  int c1;

  fb = 0;
  while ((c1 = __VERIFIER_nondet_int ()) != -1)
  {
    if (isascii (c1) && isspace (c1))
      continue;
    if (c1 == '=')
      continue;


    ((fb < 2) ? (void) (0) : __assert_fail ("fb < 2", "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c", 22, __PRETTY_FUNCTION__));

    fbuf[fb] = c1;


    if (fbuf[fb] == '\n' || fb >= 2)
    {
      fb--;
      if (fb < 0)
 fb = 0;
      else if (fbuf[fb] != '\r')
 fb++;


      ((fb <= 2) ? (void) (0) : __assert_fail ("fb <= 2", "../versisec/sendmail/progs/sendmailCVE-1999-0047mime7to8_mime7to8_arr_one_char_heavy_test_ok.c", 36, __PRETTY_FUNCTION__));

      fbuf[fb] = 0;
      fb = 0;
    }
    else
      fb++;
  }


  if (fb > 0)
  {

    fbuf[fb] = 0;
  }

  return 0;
}
