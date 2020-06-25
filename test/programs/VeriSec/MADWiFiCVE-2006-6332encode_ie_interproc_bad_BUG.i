# 1 "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c"
# 1 "../versisec/MadWiFi/progs/../constants.h" 1
# 1 "../versisec/MadWiFi/progs/../lib/stubs.h" 1



# 1 "../versisec/MadWiFi/progs/../lib/base.h" 1
# 5 "../versisec/MadWiFi/progs/../lib/stubs.h" 2
# 14 "../versisec/MadWiFi/progs/../lib/stubs.h"
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
# 2 "../versisec/MadWiFi/progs/../constants.h" 2

typedef unsigned int u_int;
typedef unsigned char u_int8_t;

struct ieee80211_scan_entry {
  u_int8_t *se_rsn_ie;
};
# 2 "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c" 2
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



# 3 "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c" 2

static u_int
encode_ie(void *buf, size_t bufsize,
               const u_int8_t *ie, size_t ielen,
        const char *leader, size_t leader_len)
{
  void *bufend = buf + bufsize;


  u_int8_t *p;
  int i;


  if (bufsize < leader_len)
    return 0;
  p = buf;
  memcpy(p, leader, leader_len);
  bufsize -= leader_len;
  p += leader_len;

  for (i = 0; i < ielen && bufsize > 2; i++) {
# 34 "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c"
    ((p < bufend) ? (void) (0) : __assert_fail ("p < bufend", "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c", 34, __PRETTY_FUNCTION__));
    *p = 'x';


    ((p+1 < bufend) ? (void) (0) : __assert_fail ("p+1 < bufend", "../versisec/MadWiFi/progs/MADWiFiCVE-2006-6332encode_ie_interproc_bad.c", 38, __PRETTY_FUNCTION__));
    *(p+1) = 'x';
    p += 2;
  }



  return (i == ielen ? p - (u_int8_t *)buf : 0);
}


static int
giwscan_cb(const struct ieee80211_scan_entry *se)
{
  u_int8_t buf[2 + 1 + 3];
  char rsn_leader [1];



  if (se->se_rsn_ie != ((void *)0)) {
    if (se->se_rsn_ie[0] == 200)
      encode_ie(buf, sizeof(buf),
                se->se_rsn_ie, se->se_rsn_ie[1] + 2,
                rsn_leader, sizeof(rsn_leader));
  }

  return 0;
}

int main ()
{
  struct ieee80211_scan_entry se;
  u_int8_t ie [2 + 1 + 3 - 1];
  se.se_rsn_ie = ie;
  se.se_rsn_ie[0] = 200;
  se.se_rsn_ie[1] = 2 + 1 + 3 - 1 - 2;

  giwscan_cb (&se);

  return 0;
}
