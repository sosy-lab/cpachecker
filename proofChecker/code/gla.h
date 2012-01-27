#ifndef GLA_H
#define GLA_H

/* $Id: gla.h,v 1.11 1998/07/24 15:26:12 waite Exp $ */
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

#if defined(__cplusplus) || defined(__STDC__)
extern int glalex(int *v);	/* v: pointer to storage for intrinsic value */
extern void lexerr(char *, int, int *, int *);
#else
extern int glalex();		/* scanner routine */
extern void lexerr();		/* Token processor for lexical errors */
#endif

extern int ResetScan;
extern char *StartLine;
extern char *TokenStart;
extern char *TokenEnd;

#define NORETURN 15001

#endif
