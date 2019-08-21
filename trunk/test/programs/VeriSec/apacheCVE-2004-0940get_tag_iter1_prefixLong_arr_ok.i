# 1 "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c"
# 1 "../versisec/apache/progs/../apache.h" 1
# 1 "../versisec/apache/progs/../lib/stubs.h" 1



# 1 "../versisec/apache/progs/../lib/base.h" 1
# 5 "../versisec/apache/progs/../lib/stubs.h" 2
# 14 "../versisec/apache/progs/../lib/stubs.h"
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
# 2 "../versisec/apache/progs/../apache.h" 2



int ap_isspace(char c);
int ap_tolower(char c);
char * ap_cpystrn(char *dst, const char *src, size_t dst_size);



extern int __VERIFIER_nondet_char ();
# 2 "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c" 2
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



# 3 "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c" 2

char *get_tag(char *tag, int tagbuf_len)
{
  char *tag_val, c, term;
  int t;

  t = 0;

  --tagbuf_len;

  do {
    {c = __VERIFIER_nondet_char();};
  } while (ap_isspace(c));

  if (c == '-') {
    {c = __VERIFIER_nondet_char();};
    if (c == '-') {
      do {
        {c = __VERIFIER_nondet_char();};
      } while (ap_isspace(c));
      if (c == '>') {
        ap_cpystrn(tag, "done", tagbuf_len);
        return tag;
      }
    }
    return ((void *)0);
  }

  while (1) {
    if (t == tagbuf_len) {
      tag[t] = 0;
      return ((void *)0);
    }
    if (c == '=' || ap_isspace(c)) {
      break;
    }
    tag[t] = ap_tolower(c);
    t++;
    {c = __VERIFIER_nondet_char();};
  }

  tag[t] = 0;
  t++;
  tag_val = tag + t;

  while (ap_isspace(c)) {
    {c = __VERIFIER_nondet_char();};
  }
  if (c != '=') {
    return ((void *)0);
  }

  do {
    {c = __VERIFIER_nondet_char();};
  } while (ap_isspace(c));

  if (c != '"' && c != '\'') {
    return ((void *)0);
  }
  term = c;
  while (1) {
    {c = __VERIFIER_nondet_char();};
    if (t == tagbuf_len) {
      tag[t] = 0;
      return ((void *)0);
    }

    if (c == '\\') {
      {c = __VERIFIER_nondet_char();};
      if (c != term) {

 ((t + 1 < tagbuf_len) ? (void) (0) : __assert_fail ("t + 1 < tagbuf_len", "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c", 74, __PRETTY_FUNCTION__));


        tag[t] = '\\';
        t++;
        if (t == tagbuf_len) {

          tag[t] = 0;
          return ((void *)0);
        }
      }
    }
    else if (c == term) {
      break;
    }


    ((t + 2 < tagbuf_len) ? (void) (0) : __assert_fail ("t + 2 < tagbuf_len", "../versisec/apache/progs/apacheCVE-2004-0940get_tag_iter1_prefixLong_arr_ok.c", 91, __PRETTY_FUNCTION__));


    tag[t] = c;
    t++;

  }

  tag[t] = 0;

  return tag;
}

int main ()
{
  char tag[2 + 2];


  get_tag (tag, 2 + 2);

  return 0;
}
