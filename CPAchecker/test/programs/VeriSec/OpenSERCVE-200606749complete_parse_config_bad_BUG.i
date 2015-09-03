# 1 "../versisec/OpenSer/progs/OpenSERCVE-200606749complete_parse_config_bad.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/OpenSer/progs/OpenSERCVE-200606749complete_parse_config_bad.c"
# 1 "../versisec/OpenSer/progs/../constants.h" 1



# 1 "../versisec/OpenSer/progs/../lib/stubs.h" 1



# 1 "../versisec/OpenSer/progs/../lib/base.h" 1
# 5 "../versisec/OpenSer/progs/../lib/stubs.h" 2
# 14 "../versisec/OpenSer/progs/../lib/stubs.h"
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
# 5 "../versisec/OpenSer/progs/../constants.h" 2
# 2 "../versisec/OpenSer/progs/OpenSERCVE-200606749complete_parse_config_bad.c" 2
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



# 3 "../versisec/OpenSer/progs/OpenSERCVE-200606749complete_parse_config_bad.c" 2

static int parse_expression_list(char *str)
{
  int start=0, i=-1, j=-1, apost=0;
  char str2[2];

  if (!str) return -1;

  do {


    i++;
    switch(str[i]) {
    case '"': apost = !apost;
      break;



    case ',': if (apost) break;
    case 0:


      while ((str[start] == ' ') || (str[start] == '\t')) start++;


      if (str[start] == '"') start++;


      j = i-1;


      while ((0 < j) && ((str[j] == ' ') || (str[j] == '\t'))) j--;
      if ((0 < j) && (str[j] == '"')) j--;


      if (start<=j) {


        ((j-start+1 < 2) ? (void) (0) : __assert_fail ("j-start+1 < 2", "../versisec/OpenSer/progs/OpenSERCVE-200606749complete_parse_config_bad.c", 41, __PRETTY_FUNCTION__));
        r_strncpy(str2, str+start, j-start+1);

        str2[j-start+1] = 0;
      } else {

        return -1;
      }

      start = i+1;
    }
  } while (str[i] != 0);

  return 0;
}

int parse_expression (char *str) {
  char *except;
  char str2 [2 + 2 + 4];

  except = strstr(str, "EX");
  if (except) {
    strncpy(str2, str, except-str);
    str2[except-str] = 0;
    if (parse_expression_list(except+2)) {

      return -1;
    }
  }

  return 0;
}

int main ()
{
  char A [2 + 2 + 4 +1];
  A[2 + 2 + 4] = 0;

  parse_expression (A);
  return 0;
}
