static char RCSid[] = "$Id: str.c,v 1.13 2009/09/09 19:31:57 profw Exp $";
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

#include "csm.h"

/***/
#if defined(__cplusplus) || defined(__STDC__)
void
mkstr(const char *c, int l, int *t, int *p)
#else
void
mkstr(c, l, t, p)
char *c; int l, *t; int *p;
#endif
/* Make an internal string value from a character string
 *    On entry-
 *       c points to a character string of length l
 *       t points to a location containing the initial terminal code
 *    On exit-
 *       The proper terminal code for the string
 *          has been stored at the location pointed to by t
 *       An internal string value representing the character string
 *          has been stored at the location pointed to by p
 ***/
{
	*p = stostr(c, l);
}
