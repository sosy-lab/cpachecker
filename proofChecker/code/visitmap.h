#ifndef VISITMAP_H
#define VISITMAP_H

#include "nodeptr.h"

#if defined(__STDC__) || defined(__cplusplus)
typedef void (* _VPROCPTR) (NODEPTR);
#else
typedef void (* _VPROCPTR) ();
#endif

extern _VPROCPTR VS1MAP[];

#endif

