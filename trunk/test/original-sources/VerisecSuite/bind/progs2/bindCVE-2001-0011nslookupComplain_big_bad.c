#include "../bind1.h"
#include <assert.h>

extern int nondet_int();

/* Avoid mallocing databufs and namebufs. */
struct databuf dummybuf;
struct namebuf dummyNameBuf;
struct databuf dummybuf2;

struct	timeval tt;

/* Another completely unsound stub brought to you by the Verisec Project. */
const char *p_type (int type)
{
  return NULL;
}

/* Since I'm not providing a complete model, this'll complain about some
 * invalid pointer dereferences.
 */
int haveComplained (const char *tag1,
                    const char *tag2)
{
  struct complaint {
    const char *tag1, *tag2;
    time_t expire;
    struct complaint *next;
  };
  static struct complaint *List = NULL;
  struct complaint *cur, *next, *prev;
  int r = 0;
  struct complaint dummy;

  for (cur = List, prev = NULL;  cur;  prev = cur, cur = next) {
    next = cur->next;
    if (tt.tv_sec > cur->expire) {
      if (prev)
        prev->next = next;
      else
        List = next;
      //free((char*) cur);
      cur = prev;
    } else if ((tag1 == cur->tag1) && (tag2 == cur->tag2)) {
      r++;
    }
  }
  if (!r) {
    cur = &dummy;//(struct complaint *)malloc(sizeof(struct complaint));
    if (cur) {
      cur->tag1 = tag1;
      cur->tag2 = tag2;
      cur->expire = tt.tv_sec + INIT_REFRESH;	/* "10:00" */
      cur->next = NULL;
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
  char queryname[INSZ+1], dname[INSZ+1];
  const char *a, *ns;
  const char *a_type;
  int print_a;

  strncpy(queryname, net_queryname, sizeof queryname);
  queryname[(sizeof queryname) - 1] = EOS;
  strncpy(dname, net_dname, sizeof dname);
  dname[(sizeof dname) - 1] = EOS;
  
  if (sysloginfo && queryname && !haveComplained(queryname, complaint)) {
    char buf[BUFSZ];
    
    a = ns = (char *)NULL;
    print_a = (a_rr->d_type == T_A);
    a_type = p_type(a_rr->d_type);
    
    /*von mir eingefügt*/
    assert(BUFSZ < strlen(sysloginfo));
    /* BAD */
    r_strcpy (buf, sysloginfo);
  }
}

int
match(struct databuf *dp,
      int class, 
      int type)
{
  if (dp->d_class != class && class != C_ANY)
    return (0);
  if (dp->d_type != type && type != T_ANY)
    return (0);
  return (1);
}

/* Really dumb stub. */
struct namebuf *
nlookup(const char *name,
	struct hashbuf **htpp,
	const char **fname,
	int insert)
{
  dummyNameBuf.n_data = &dummybuf2;
  /* I suspect SatAbs assumes globals are initialized to 0 -- I really
     shouldn't need to be hard-wiring these. - Tom */
  dummybuf2.d_type = T_NS;
  dummybuf2.d_zone = 1;
  return &dummyNameBuf;
}

int
findMyZone(struct namebuf *np,
           int class)
{
  return nondet_int ();
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
    if (nsdp == NULL) break;
    class = nsdp->d_class;
    dname = (char *)nsdp->d_data;

    for (i = 0; i < qp->q_nusedns; i++) {
      if (qp->q_usedns[i] == nsdp) {
        goto skipserver;
      }
    }

    /* Heap-allocated pointers seem to break SatAbs. */
    /*tmphtp = ((nsdp->d_flags & DB_F_HINT) ?fcachetab :hashtab);*/
    np = nlookup(dname, &tmphtp, &fname, 1);
    if (np == NULL) {
      found_arr = 0;
      goto need_sysquery;
    }
    if (fname != dname) {
      if (findMyZone(np, class) == DB_Z_CACHE) {
        assert (np != NULL);
        for (; np != NULL; /*np = np_parent(np)*/) {
          for (dp = np->n_data; dp; dp = dp->d_next) {
            if (match(dp, class, T_NS)) {
              /* SatAbs won't accept just "if (dp->d_zone)" :-/ */
              if (dp->d_zone != 0) {
                //assert (0);
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

  /* Totally bogus GOTO targets. */
 need_sysquery:
 skipserver:
  return (n - naddr);

}


int main ()
{
  struct databuf *nsp[2];
  struct qinfo qp;
  char sysloginfo [INSZ];
  char syslogdname [INSZ];

  nsp[0] = &dummybuf;
  nsp[1] = NULL;

  sysloginfo[INSZ-1] = EOS;
  syslogdname[INSZ-1] = EOS;
  

  nslookup(nsp,
           &qp,
           &sysloginfo,
           &syslogdname);

  return 0;
}
