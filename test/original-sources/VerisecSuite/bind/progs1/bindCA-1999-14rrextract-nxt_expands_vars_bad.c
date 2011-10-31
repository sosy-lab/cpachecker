#include "../bind.h"
#include <assert.h>

#define SPACE_FOR_VARS 2*INT16SZ + INT32SZ + INT16SZ

static int
rrextract(u_char *msg, int msglen, u_char *rrp, u_char *dname, int namelen)
{
  u_char *eom, *cp, *cp1, *rdatap;
  u_int class, type, dlen;
  int n;
  u_int32_t ttl;
  u_char data[MAXDATA*2 + SPACE_FOR_VARS];
  data [(MAXDATA*2) + SPACE_FOR_VARS - 1] = EOS;

  cp = rrp;
  eom = msg + msglen;

  if ((n = dn_expand(msg, eom, cp, (char *) dname, namelen)) < 0) {
    return (-1);
  }
  
  cp += n;
  BOUNDS_CHECK(cp, 2*INT16SZ + INT32SZ + INT16SZ);
  GETSHORT(type, cp);
  GETSHORT(class, cp);
  
  if (class > CLASS_MAX) {
    return (-1);
  }
  GETLONG(ttl, cp);

  if (ttl > MAXIMUM_TTL) {
    ttl = 0;
  }
  GETSHORT(dlen, cp);
  BOUNDS_CHECK(cp, dlen);

  rdatap = cp;

  if (nondet_int()) {
    return (-1);
  }


  /* Cut the switch.... */

  n = dn_expand(msg, eom, cp, (char *)data, sizeof data);
	   
  if (n < 0 || n >= dlen) {
    return (-1);
  }

  if (nondet_int()) {
    return (-1);
  }
  cp += n;
  cp1 = data + strlen((char *)data) + 1;

  /* von mir eingefügt*/
  assert(dlen - n <= sizeof data - (strlen((char *)data) + 1));
  /* BAD */
  r_memcpy(cp1, cp, dlen - n);

  return 0;
}

int main(){
  
  int msglen, ret;
  u_char *dp;
  u_char name [NAMELEN];
  u_char msg [MSGLEN+ + SPACE_FOR_VARS];

  name [NAMELEN-1] = EOS;
  msg [MSGLEN + SPACE_FOR_VARS - 1] = EOS;

  msglen = MSGLEN;
  dp = msg;

  ret = rrextract(msg, msglen, dp, name, NAMELEN);
  
  return 0;
  
}
