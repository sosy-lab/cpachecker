#ifndef PDL_GEN_H
#define PDL_GEN_H

#include "deftbl.h"

#ifdef ELI_ARG
#undef ELI_ARG
#endif

#if defined(__STDC__) || defined(__cplusplus)
#define ELI_ARG(proto)    proto
#else
#define ELI_ARG(proto)    ()
#endif


#define TYPE int
extern TYPE Getint ELI_ARG((int _Property, DefTableKey key, TYPE deflt));
extern void Setint ELI_ARG((int _Property, DefTableKey key, TYPE _add, TYPE _replace));
extern void Resetint ELI_ARG((int _Property, DefTableKey key, TYPE _val));
#undef TYPE

#define SetClpValue(key, _add, _replace)   \
		Setint(1, (key), (_add), (_replace))
#define ResetClpValue(key, _val)   \
		Resetint(1, (key), (_val))
#define GetClpValue(key, deflt)   \
		Getint(1, (key), (deflt))

extern struct PropList PDLk[];


extern DefTableKey CloneKey ELI_ARG((DefTableKey key));
/* Clone a definition
 *    On entry-
 *       key=a valid definition
 *    On exit-
 *       CloneKey=Unique definition with the same properties
 *                and property values as key
 ***/

#ifdef MONITOR
extern void pdl_mon_properties ELI_ARG((DefTableKey));
#endif

#endif
