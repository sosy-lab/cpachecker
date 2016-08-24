# 1 "../versisec/bind/progs2/bindCVE-2001-0011nslookupComplain_big_ok.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../versisec/bind/progs2/bindCVE-2001-0011nslookupComplain_big_ok.c"
# 1 "../versisec/bind/progs2/../bind1.h" 1
# 1 "../versisec/bind/progs2/../lib/stubs.h" 1



# 1 "../versisec/bind/progs2/../lib/base.h" 1
# 5 "../versisec/bind/progs2/../lib/stubs.h" 2
# 14 "../versisec/bind/progs2/../lib/stubs.h"
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
# 2 "../versisec/bind/progs2/../bind1.h" 2

int haveComplained (const char *tag1, const char *tag2);
const char *p_type (int type);
# 25 "../versisec/bind/progs2/../bind1.h"
typedef char u_char;
typedef short int16_t;
typedef int time_t;
typedef int u_int;
typedef int uint32_t;
typedef long u_long;

struct databuf {
  struct databuf *d_next;
  int16_t d_type;
  u_char d_data[sizeof(char*)];
  int16_t d_class;
  int d_flags;
  int16_t d_zone;
};

struct namebuf {
  u_int n_hashval;
  struct namebuf *n_next;
  struct databuf *n_data;
  struct namebuf *n_parent;
  struct hashbuf *n_hash;
  char _n_name[sizeof(void*)];
};

struct timeval
{
  time_t tv_sec;
};


typedef uint32_t in_addr_t;
struct in_addr {
  in_addr_t s_addr;
};

struct sockaddr_in {
  short sin_family;
  unsigned short sin_port;
  struct in_addr sin_addr;
};


struct qinfo {
  struct databuf *q_usedns[16];
  u_char q_naddr;
  u_char q_nusedns;
};
# 2 "../versisec/bind/progs2/bindCVE-2001-0011nslookupComplain_big_ok.c" 2
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



# 3 "../versisec/bind/progs2/bindCVE-2001-0011nslookupComplain_big_ok.c" 2

extern int __VERIFIER_nondet_int();


struct databuf dummybuf;
struct namebuf dummyNameBuf;
struct databuf dummybuf2;

struct timeval tt;


const char *p_type (int type)
{
  return ((void *)0);
}




int haveComplained (const char *tag1,
                    const char *tag2)
{
  struct complaint {
    const char *tag1, *tag2;
    time_t expire;
    struct complaint *next;
  };
  static struct complaint *List = ((void *)0);
  struct complaint *cur, *next, *prev;
  int r = 0;
  struct complaint dummy;

  for (cur = List, prev = ((void *)0); cur; prev = cur, cur = next) {
    next = cur->next;
    if (tt.tv_sec > cur->expire) {
      if (prev)
        prev->next = next;
      else
        List = next;

      cur = prev;
    } else if ((tag1 == cur->tag1) && (tag2 == cur->tag2)) {
      r++;
    }
  }
  if (!r) {
    cur = &dummy;
    if (cur) {
      cur->tag1 = tag1;
      cur->tag2 = tag2;
      cur->expire = tt.tv_sec + 600;
      cur->next = ((void *)0);
      if (prev)
        prev->next = cur;
      else
        List = cur;
    }
  }
  return (r);

}

static void
nslookupComplain(const char *sysloginfo,
                 const char *net_queryname,
                 const char *complaint,
                 const char *net_dname,
                 const struct databuf *a_rr,
                 const struct databuf *nsdp)
{
  char queryname[2 + 3 +1], dname[2 + 3 +1];
  const char *a, *ns;
  const char *a_type;
  int print_a;

  strncpy(queryname, net_queryname, sizeof queryname);
  queryname[(sizeof queryname) - 1] = 0;
  strncpy(dname, net_dname, sizeof dname);
  dname[(sizeof dname) - 1] = 0;

  if (sysloginfo && queryname && !haveComplained(queryname, complaint)) {
    char buf[2];

    a = ns = (char *)((void *)0);
    print_a = (a_rr->d_type == 1);
    a_type = p_type(a_rr->d_type);




    r_strncpy (buf, sysloginfo, 2);
  }
}

int
match(struct databuf *dp,
      int class,
      int type)
{
  if (dp->d_class != class && class != 255)
    return (0);
  if (dp->d_type != type && type != 255)
    return (0);
  return (1);
}


struct namebuf *
nlookup(const char *name,
 struct hashbuf **htpp,
 const char **fname,
 int insert)
{
  dummyNameBuf.n_data = &dummybuf2;


  dummybuf2.d_type = 2;
  dummybuf2.d_zone = 1;
  return &dummyNameBuf;
}

int
findMyZone(struct namebuf *np,
           int class)
{
  return __VERIFIER_nondet_int ();
}


int
nslookup(struct databuf *nsp[],
         struct qinfo *qp,
         const char *syslogdname,
         const char *sysloginfo)
{
  struct namebuf *np;
  struct databuf *dp, *nsdp;
  struct qserv *qs;
  int n;
  u_int i;
  struct hashbuf *tmphtp;
  char *dname;
  const char *fname;
  int oldn, naddr, class, found_arr, potential_ns;
  time_t curtime;


  potential_ns = 0;
  n = qp->q_naddr;
  naddr = n;
  curtime = (u_long) tt.tv_sec;
  while (1) {
    nsdp = *nsp;
    nsp++;
    if (nsdp == ((void *)0)) break;
    class = nsdp->d_class;
    dname = (char *)nsdp->d_data;

    for (i = 0; i < qp->q_nusedns; i++) {
      if (qp->q_usedns[i] == nsdp) {
        goto skipserver;
      }
    }



    np = nlookup(dname, &tmphtp, &fname, 1);
    if (np == ((void *)0)) {
      found_arr = 0;
      goto need_sysquery;
    }
    if (fname != dname) {
      if (findMyZone(np, class) == (0)) {
        ((np != ((void *)0)) ? (void) (0) : __assert_fail ("np != ((void *)0)", "../versisec/bind/progs2/bindCVE-2001-0011nslookupComplain_big_ok.c", 176, __PRETTY_FUNCTION__));
        for (; np != ((void *)0); ) {
          for (dp = np->n_data; dp; dp = dp->d_next) {
            if (match(dp, class, 2)) {

              if (dp->d_zone != 0) {

                static char *complaint =
                  "Glue A RR missing";
                nslookupComplain(sysloginfo,
                                 syslogdname,
                                 complaint,
                                 dname, dp,
                                 nsdp);
                goto skipserver;
              } else {
                found_arr = 0;
                goto need_sysquery;
              }
            }
          }
        }
        found_arr = 0;
        goto need_sysquery;
      } else {
        continue;
      }
    }

    break;
  }


 need_sysquery:
 skipserver:
  return (n - naddr);

}


int main ()
{
  struct databuf *nsp[2];
  struct qinfo qp;
  char sysloginfo [2 + 3];
  char syslogdname [2 + 3];

  nsp[0] = &dummybuf;
  nsp[1] = ((void *)0);

  sysloginfo[2 + 3 -1] = 0;
  syslogdname[2 + 3 -1] = 0;


  nslookup(nsp,
           &qp,
           &sysloginfo,
           &syslogdname);

  return 0;
}
