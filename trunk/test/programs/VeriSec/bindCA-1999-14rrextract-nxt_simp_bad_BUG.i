# 1 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
# 1 "../versisec/bind/progs1/../bind.h" 1
# 1 "../versisec/bind/progs1/../lib/stubs.h" 1



# 1 "../versisec/bind/progs1/../lib/base.h" 1
# 5 "../versisec/bind/progs1/../lib/stubs.h" 2
# 14 "../versisec/bind/progs1/../lib/stubs.h"
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
# 2 "../versisec/bind/progs1/../bind.h" 2





typedef char u_char;
typedef int u_int;
typedef int u_int32_t;
# 46 "../versisec/bind/progs1/../bind.h"
int dn_expand(const u_char *msg, const u_char *eomorig,
              const u_char *comp_dn, char *exp_dn, int length);


extern int __VERIFIER_nondet_int();
# 2 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c" 2
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



# 3 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c" 2



static int
rrextract(u_char *msg, int msglen, u_char *rrp, u_char *dname, int namelen)
{
  u_char *eom, *cp, *cp1, *rdatap;
  u_int class, type, dlen;
  int n;
  u_char data[2*2 + 2];
  data [(2*2 + 2)-1] = 0;

  cp = rrp;
  eom = msg + msglen;

  do {(dlen) = __VERIFIER_nondet_short(); (cp) += 2;} while(0);
  do {if ((cp) + (dlen) > eom) return -1;} while(0);



  n = dn_expand(msg, eom, cp, (char *)data, sizeof data);

  if (n < 0 || n >= dlen) {
    return (-1);
  }

  if (nondet_int()) {
    return (-1);
  }
  cp += n;
  cp1 = data + strlen((char *)data) + 1;


  ((dlen - n <= sizeof data - (strlen((char *)data) + 1)) ? (void) (0) : __assert_fail ("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c", 36, __PRETTY_FUNCTION__));

  r_memcpy(cp1, cp, dlen - n);

  return 0;
}

int main(){

  int msglen, ret;
  u_char *dp;
  u_char name [3];
  u_char msg [2 + 2 + 2];

  name [3 -1] = 0;
  msg [2 + 2 + 2 - 1] = 0;

  msglen = 2 + 2;
  dp = msg;

  ret = rrextract(msg, msglen, dp, name, 3);

  return 0;

}
