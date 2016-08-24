#include "lib/stubs.h"

typedef unsigned int u_int;
typedef unsigned char u_int8_t;

struct ieee80211_scan_entry {
  u_int8_t *se_rsn_ie;            /* captured RSN ie */
};

#define IEEE80211_ELEMID_RSN 200 /* fake */

/* Size of an array leader[] which is written to buf[] before it is
 * overflowed by the ie[] array. */
#define LEADERSZ 1

/* We first write the "leader" to buf[], and then write from the "ie"
 * array. buf[] has to be bigger than LEADERSZ by at least 2. */
#define BUFSZ BASE_SZ + LEADERSZ + 3

/* Just has to be big enough to overflow buf[]
 * Note that for each byte in ie[], two bytes are written to buf[] in
 * encode_ie() */
#define IESZ BUFSZ - LEADERSZ
