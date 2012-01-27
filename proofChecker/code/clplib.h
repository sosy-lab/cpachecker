#ifndef CLPLIB_DEF
#define CLPLIB_DEF

/*
 * $Id: clplib.h,v 1.11 1999/03/01 05:15:12 tony Exp $
 * Copyright (c) 1991, The Regents of the University of Colorado
 * Copyright (c) 1994-1997, Anthony M. Sloane
 */

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

/*
 * clp_init
 * Initialize the command line relations.
 */

extern void clp_init ELI_ARG((int argc, char *argv[]));

/*
 * clp_string
 * Return a new string value.
 */

extern int clp_string ELI_ARG((char *s));

#endif
