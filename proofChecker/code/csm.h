#ifndef CSM_H
#define CSM_H
/* $Id: csm.h,v 3.4 1999/03/11 15:18:45 mjung Exp $ */
/* Copyright 1989, The Regents of the University of Colorado */

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

#include <stdio.h>
#include "obstack.h"
#include "eliproto.h"

	/* Variables exported by the Character Storage Module */

extern char **strng;			/* Pointers to the stored strings */
extern int numstr;			/* Number of strings stored */
extern struct obstack csm_obstack;	/* String data storage */
extern char *CsmStrPtr;			/* String stored in Csm_obstk */


	/* Macros defined by the Character Storage Module */

#define NoStrIndex 0
#define Csm_obstk (&csm_obstack)
#define NoStr ((char *)0)


	/* Routines exported by the Character Storage Module */

extern char * StringTable ELI_ARG((int i));
/* Obtain a string from the string table
 *   On entry-
 *     i indexes the desired string
 *   On exit-
 *     StringTable points to the indexed string
 ***/

extern void prtstcon ELI_ARG((FILE *d, const char *p));
/* Print a sequence of characters as a string constant without quotes
 *    On exit-
 *       The string pointed to by p has been added to the current
 *          line of d as a string constant without quotes
 ***/


extern void savestr ELI_ARG((FILE *d));
/* Save the current string table state
 *    On exit-
 *       File d is a symbolic encoding of the current state, suitable
 *          for inclusion in the string table module
 ***/


extern void dmpstr ELI_ARG((FILE *d));
/* Dump the string table
 *    On exit-
 *       The string table contents have been written to d
 ***/


extern int stostr ELI_ARG((const char *c, int l));
/* Store a string in the string table
 *    On entry-
 *       c points to the string to be stored
 *       l>0 = length of the string to be stored
 *    On exit-
 *       stostr=string table index of the stored string
 ***/

#endif
