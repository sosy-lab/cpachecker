#ifndef IDN_H
#define IDN_H
/* $Id: idn.h,v 1.15 2009/09/09 19:31:57 profw Exp $ */
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

#include "eliproto.h"
#include <stdio.h>

	/* Type exported by the Identifier Table Module */

typedef int INTERNIDN;
#define NoIdn 0


#ifndef NOFOLD
	/* Variable exported by the Identifier Table Module */

extern int dofold;
#endif


	/* Routines exported by the Identifier Table Module */

extern void prtidnv ELI_ARG((FILE *d, int i));
/* Print an identifier
 *    On exit-
 *       The identifier encoded by i has been added to the current
 *          line of d.
 ***/


extern void saveidn ELI_ARG((FILE *d));
/* Save the current identifier table state
 *    On exit-
 *       File d is a symbolic encoding of the current state, suitable
 *          for inclusion in the identifier table module
 ***/


extern void dmpidn ELI_ARG((FILE *d));
/* Dump the identifier table
 *    On exit-
 *       The entire identifier table has been written to d
 ***/


extern void mkidn ELI_ARG((const char *c, int l, int *t, int *s));
/* Obtain the internal coding of an identifier or keyword
 *    On entry-
 *       c points to the identifier or keyword
 *       l=length of the identifier or keyword
 *       t points to a location containing the initial terminal code
 *    If the identifier or keyword has appeared previously then on exit-
 *       t has been set to the terminal code given on its first appearance
 *       s has been set to the internal coding set on its first appearance
 *    Otherwise on exit-
 *       t remains unchanged
 *       s has been set to a new internal coding
 ***/

#endif
