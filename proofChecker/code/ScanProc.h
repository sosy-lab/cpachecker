#ifndef SCANPROC_H
#define SCANPROC_H
/* $Id: ScanProc.h,v 2.2 2009/09/09 19:31:57 profw Exp $ */
/* Copyright 1999, The Regents of the University of Colorado */

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

/* Prototypes for all pre-defined auxiliary scanners */

extern char *auxNUL ELI_ARG((char *, int));
extern char *auxEOF ELI_ARG((char *, int));
extern char *auxNewLine ELI_ARG((char *, int));
extern char *auxTab ELI_ARG((char *, int));
extern char *coordAdjust ELI_ARG((char *, int));
extern char *auxEOL ELI_ARG((char *, int));
extern char *auxNoEOL ELI_ARG((char *, int));

extern char *auxCString ELI_ARG((char *, int));
extern char *auxCChar ELI_ARG((char *, int));
extern char *auxCComment ELI_ARG((char *, int));
extern char *Ctext ELI_ARG((char *, int));

extern char *auxPascalString ELI_ARG((char *, int));
extern char *auxPascalComment ELI_ARG((char *, int));

extern char *auxM2String ELI_ARG((char *, int));
extern char *auxM3Comment ELI_ARG((char *, int));


/* Prototypes for all pre-defined token processors */

extern void EndOfText ELI_ARG((const char *c, int length, int *t, int *v));

extern void mkidn ELI_ARG((const char *c, int length, int *t, int *v));
extern void mkint ELI_ARG((const char *c, int length, int *t, int *v));
extern void mkstr ELI_ARG((const char *c, int length, int *t, int *v));

extern void c_mkstr ELI_ARG((const char *c, int length, int *t, int *v));
extern void c_mkchar ELI_ARG((const char *c, int length, int *t, int *v));
extern void c_mkint ELI_ARG((const char *c, int length, int *t, int *v));

extern void modula_mkint ELI_ARG((const char *c, int length, int *t, int *v));

#endif
