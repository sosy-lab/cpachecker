#ifndef TABSIZE_H
#define TABSIZE_H

/* $Id: tabsize.h,v 1.4 1997/09/04 18:20:55 waite Exp $ */
/* Copyright 1993, The Regents of the University of Colorado */

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

/* Distance to the next tab stop
 *   On entry-
 *     p=index of the column beyond the tab in the entire source line
 *   On exit-
 *     TABSIZE=number of spaces that must be added to reach the next tab stop
 */
#define TABSIZE(p) TabSize(p)

#if defined(__cplusplus) || defined(__STDC__)
extern int TabSize(int);
#else
extern int TabSize();
#endif

#endif
