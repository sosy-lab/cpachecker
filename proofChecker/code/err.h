#ifndef ERR_H
#define ERR_H

/* $Id: err.h,v 1.35 2010/09/22 02:31:34 profw Exp $ */
/* Copyright 1997, The Regents of the University of Colorado */

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

#ifdef PROTO_OK
#include <stdio.h>
#endif

	/* Error report classification */

#define NOTE	0	/* Nonstandard construct */
#define COMMENT	0	/* Obsolete */
#define WARNING	1	/* Repairable error */
#define ERROR	2	/* Unrepairable error */
#define FATAL	2	/* Obsolete */
#define DEADLY	3	/* Error that makes continuation impossible */


	/* Types exported by the Error Module */

typedef struct {	/* Source listing coordinates */
	int line;	   /* Left line number */
	int col;	   /* Left character position */
#ifdef RIGHTCOORD
	int rline;	   /* Right line number */
	int rcol;	   /* Right character position */
#endif
#ifdef MONITOR
	int cumcol;	   /* Left cumulative chararcter position */
	int rcumcol;	   /* Right cumulative chararcter position */
#endif
} POSITION;

typedef POSITION *CoordPtr;

#define NoPosition	((POSITION *)0)
#define LineOf(pos)     ((pos).line)
#define ColOf(pos)      ((pos).col)
#ifdef RIGHTCOORD
#define RLineOf(pos)    ((pos).rline)
#define RColOf(pos)     ((pos).rcol)
#endif
#ifdef MONITOR
#define CumColOf(pos)	((pos).cumcol)
#define RCumColOf(pos)	((pos).rcumcol)
#endif

	/* Variables exported by the Error Module */

extern int ErrorCount[];
extern int LineNum;	/* Index of the current line in the total source text */
extern POSITION NoCoord;  /* The NULL coordinate */
extern POSITION curpos;	/* Position variable for general use */

	/* Routines exported by the Error Module */

extern void
ErrorInit ELI_ARG((int ImmOut, int AGout, int ErrLimit));
/* Initialize the error module 
 *    On entry- 
 *       ImmOut=1 if immediate error output required 
 *       AGout=1 to print AG line number on error reports 
 *       ErrLimit=1 to limit the number of errors reported
 ***/


extern void
message ELI_ARG((int severity, CONST char *Msgtext, int grammar, POSITION *source));
/* Report an error
 *    On entry-
 *      severity=error severity
 *      Msgtext=message text
 *      grammar=identification of the test that failed
 *      source=source coordinates at which the error was detected
 ***/


extern void
lisedit ELI_ARG((CONST char *name, FILE *stream, int cutoff, int erronly));
/* Output the listing with embedded error messages
 *    On entry-
 *       name is the source file name
 *       stream specifies the listing file
 *       cutoff=lowest severity level that will be listed
 *    If erronly != 0 then on exit-
 *       Source file lines containing errors have been added to file stream
 *          with error messages attached
 *    Else on exit-
 *       All source file lines have been added to file stream
 *          with error messages attached to those containing errors
 ***/

#ifdef MONITOR
/* Monitoring support for structured values */

#define DAPTO_RESULTCoordPtr(p) DAPTO_RESULT_PTR(p)
#define DAPTO_ARGCoordPtr(p)    DAPTO_ARG_PTR(p, CoordPtr)

#define DAPTO_RESULTPOSITION(p) \
    DAPTO_RESULT_STR (_dap_format ("%d,%d-%d,%d", LineOf (p), ColOf (p), RLineOf (p), RColOf (p)))

#endif

#endif
