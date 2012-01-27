/* $Id: Strings.h,v 3.4 1999/11/17 15:05:22 uwe Exp $ */
/* Copyright, 1992, AG-Kastens, University Of Paderborn */

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





#ifndef STRINGS_H
#define STRINGS_H

#include "csm.h"

typedef CONST char *CharPtr;

#if defined(__cplusplus) || defined(__STDC__)
extern CharPtr CatStrStr (CharPtr, CharPtr);
extern int IndCatStrStr (CharPtr, CharPtr);
#else
extern CharPtr CatStrStr ();
extern int IndCatStrStr ();
#endif

#define CatStrInd(s,i) (CatStrStr((s),StringTable(i)))

#ifdef MONITOR
#define DAPTO_RESULTCharPtr(s) DAPTO_RESULT_STR(s)
#endif

#endif
