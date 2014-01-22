#include "lib/stubs.h"

int haveComplained (const char *tag1, const char *tag2);
const char *p_type (int type);

/* XXX static function declaration limits visibility of function name to the
 * file it's declared in, so it doesn't make sense to include it in a header
 * file.
static void
nslookupComplain(const char *sysloginfo, 
                 const char *net_queryname, 
                 const char *complaint, 
                 const char *net_dname, 
                 const struct databuf *a_rr, 
                 const struct databuf *nsdp);
*/


/* Size of the buffer being overflowed. */
#define BUFSZ BASE_SZ

/* Size of the input buffer. */
#define INSZ BUFSZ + 3

typedef char u_char;
typedef short int16_t;
typedef int time_t;
typedef int u_int;
typedef int uint32_t;
typedef long u_long;

struct databuf {
  struct databuf	*d_next;	/* linked list */
  int16_t		d_type;		/* type number */
  u_char		d_data[sizeof(char*)]; /* malloc'd (padded) */
  int16_t		d_class;	/* class number */
  int           	d_flags;	/* see below */
  int16_t		d_zone;		/* zone number or 0 for the cache */
};

struct namebuf {
  u_int	        	n_hashval;	/* hash value of n_dname */
  struct namebuf	*n_next;	/* linked list */
  struct databuf	*n_data;	/* data records */
  struct namebuf	*n_parent;	/* parent domain */
  struct hashbuf	*n_hash;	/* hash table for children */
  char		        _n_name[sizeof(void*)];	/* Counted str, malloc'ed. */
};

struct timeval
{
  time_t tv_sec;            /* Seconds.  */
};

/* Internet address.  */
typedef uint32_t in_addr_t;
struct in_addr {
  in_addr_t s_addr;
};

struct sockaddr_in {
  short            sin_family;   // e.g. AF_INET
  unsigned short   sin_port;     // e.g. htons(3490)
  struct in_addr   sin_addr;     // see struct in_addr, below
};

#define NSMAX 16 /* from bind's ns_defs.h */
struct qinfo {
  struct databuf	*q_usedns[NSMAX]; /* databuf for NS that we've tried */
  u_char		q_naddr;	/* number of addr's in q_addr */
  u_char		q_nusedns;	/* number of elements in q_usedns[] */
};

#define T_A            1 
#define T_NS           2

#define	INIT_REFRESH	600	/* retry time for initial secondary */
				/* contact (10 minutes) */

#define NAME(nb)    ((nb)._n_name + 1)

#define C_ANY          255             /* wildcard match */
#define T_ANY          255             /* wildcard match */

/*
 * d_flags definitions
 */
#define DB_F_HINT       0x01		/* databuf belongs to fcachetab */
#define DB_F_ACTIVE     0x02		/* databuf is linked into a cache */

#define DB_Z_CACHE      (0)	/* cache-zone-only db_dump() */
