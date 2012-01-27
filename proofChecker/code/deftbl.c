static char rcsid[] = "$Id: deftbl.c,v 3.2 2000/08/21 14:41:17 cschmidt Exp $";
/* Property list module
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

#include <stdlib.h>
#include "err.h"
#include "deftbl.h"
#include "obstack.h"
#ifndef NORESTORE
#include "obsave.h"
#endif

#define false 0
#define true 1

static struct {char *l; void *p; char c[1];} pdl_data = {
  pdl_data.c+1,
  0,
  ""
};

static struct obstack DeftblObstack =
  obstack_known_chunk(&pdl_data, &(pdl_data.c[1]), &(pdl_data.c[1]),
    &(pdl_data.c[1]), 4096, OBSTACK_PTR_ALIGN);

/***/
DefTableKey
NewKey()
/* Establish a new definition
 *    On exit-
 *       NewKey=Unique definition table key
 ***/
{
   DefTableKey CurrDef;

   if ((CurrDef = (DefTableKey)obstack_alloc(&DeftblObstack, sizeof(Entry)))) {
      if ((CurrDef->List = (Entry)obstack_alloc(&DeftblObstack,
					       sizeof(struct PropElt)))) {
         CurrDef->List->selector = 0;
         return CurrDef;
      }
   }
   message(DEADLY, "NewKey: memory exhausted", 0, (POSITION *)0);
   return NoKey;
}

/***/
int
#if defined(__cplusplus) || defined(__STDC__)
find(DefTableKey key, int p, Entry *r, size_t add)
#else
find(key, p, r, add)
DefTableKey key; int p; Entry *r; size_t add;
#endif
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
{
   register Entry *b,t;

   if (key == NoKey) return false;
   b = &key->List; t = *b;
   while (t->selector > p) { b = &t->next; t = *b; }
   *r = t;
   if (t->selector == p) return true;
   if (add) {
      Entry temp;

      if (!(temp = (Entry)obstack_alloc(&DeftblObstack, add)))
         message(DEADLY, "find: malloc failure", 0, (POSITION *)0);
      temp->next = t; temp->selector = p;
      *r = *b = temp;
   }
   return false;
}

#ifndef NORESTORE

void *SaveModuleDeftbl()
{
	void *mark = obstack_alloc(&DeftblObstack, 1);
	
	return SaveObstack(&DeftblObstack, mark);
}

void
#if defined(__cplusplus) || defined(__STDC__) 
RestoreModuleDeftbl(void *base)
#else
RestoreModuleDeftbl(base) void *base;
#endif
{
	RestoreObstack(&DeftblObstack, base);
}

#endif
