static char rcsid[] = "$Id: dfltrepar.c,v 1.7 1997/09/04 18:21:15 waite Exp $";
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

#include "err.h"
#include "reparatur.h"

int
#if defined(__cplusplus) || defined(__STDC__)
Reparatur(POSITION *coord, int *syncode, int *intrinsic)
#else
Reparatur(coord, syncode, intrinsic)
POSITION *coord;int *syncode, *intrinsic;
#endif
/* Repair a syntax error by changing the lookahead token
 *   On entry-
 *     coord points to the coordinates of the lookahead token
 *     syncode points to the classification of the lookahead token
 *     intrinsic points to the intrinsic attribute of the lookahead token
 *   If the lookahead token has been changed then on exit-
 *     Reparatur=1
 *     coord, syncode and intrinsic reflect the change
 *   Else on exit-
 *     Reparatur=0
 *     coord, syncode and intrinsic are unchanged
 ***/
{ return 0; }
