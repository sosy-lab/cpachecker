#ifndef DEFTBL_H
#define DEFTBL_H 

/* $Id: deftbl.h,v 1.4 1995/04/07 22:28:16 kadhim Exp $ */
/* Property list module interface
   Copyright 1995, The Regents of the University of Colorado */

/* This file is part of the Eli Module Library.

The Eli Module Library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

The Eli Module Library is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with the Eli Module Library; see the file COPYING.LIB.
If not, write to the Free Software Foundation, Inc., 59 Temple Place -
Suite 330, Boston, MA 02111-1307, USA.  */

/* As a special exception, when this file is copied by Eli into the
   directory resulting from a :source derivation, you may use that
   created file as a part of that directory without restriction. */

#include <stdlib.h>	/* To get size_t */
#include "eliproto.h"

typedef struct PropElt {	/* Properties of a defined entity */
   struct PropElt *next;	   /* The next property */
   int selector;		   /* Which property */
} *Entry;

typedef struct PropList {	/* Representation of a definition table key */
   Entry List;		        /* Property list pointer */
} *DefTableKey;

#define NoKey (DefTableKey)0	/* Distinguished definition table key */

extern DefTableKey NewKey ELI_ARG((void));
/* Establish a new definition
 *    On exit-
 *       NewKey=Unique definition table key
 ***/

extern int find ELI_ARG((DefTableKey key, int p, Entry *r, size_t add));
/* Obtain a relation for a specific property of a definition
 * On entry-
 *    key=definition whose property relation is to be obtained
 *    p=selector for the desired property
 * If the definition does not have the desired property then on exit-
 *    find=false
 *    if add != 0 then r points to a new entry of size add for the property
 *    else r points to the entry following the entry for the property
 * Else on exit-
 *    find=true
 *    r points to the current entry for the property
 ***/

#ifdef MONITOR
/* Monitoring support for structured values */

#define DAPTO_RESULTDefTableKey(k) DAPTO_RESULT_PTR (k)
#define DAPTO_ARGDefTableKey(k) DAPTO_ARG_PTR (k, DefTableKey)

#endif

#endif
