static char rcsid[] = "$Id: clpintf.c,v 1.26 2009/08/11 17:06:54 profw Exp $";

/* clpintf.c
 *
 * Copyright (c) 1989, The Regents of the University of Colorado
 * Copyright (c) 1994-1997, Anthony M. Sloane
 *
 * ParseCommandLine for use with CLP.
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

#include <stdio.h>
#include <fcntl.h>
#include "clp.h"
#include "clpintf.h"
#include "csm.h"
#include "eliproto.h"
#include "pdl_gen.h"
#include "source.h"

#if defined(unix) || defined (_unix)
#include <unistd.h>
#endif

#if defined(_WIN32) || defined(MSDOS) || defined(_MSDOS)
#include <io.h>
#endif

#ifdef MONITOR
#include "clp_dapto.h"
#endif

static CONST char *fileerrmsg;
static int fileerrmsgusage;

/*
 * SetFileErr
 */

#ifdef PROTO_OK
void SetFileErr (const char *msg, int usage)
#else
void SetFileErr (msg, usage)
    char *msg;
    int usage;
#endif
{
    fileerrmsg = msg;
    fileerrmsgusage = usage;
}

/*
 * ParseCommandLine
 */

void
#ifdef PROTO_OK
ParseCommandLine (int argc, char *argv[])
#else
ParseCommandLine (argc, argv)
    int argc;
    char *argv[];
#endif
{
    CONST char *cp;

    /* set up the relations */
    clp_init (argc, argv);

    /* open the correct input stream */
    if (CLP_InputFile == NoKey) initBuf ("stdin", 0);
    else {
	CONST char *infilename = StringTable (GetClpValue (CLP_InputFile, 0));
	int infile = open (infilename, 0);
	if (infile != -1) {
	    initBuf (infilename, infile);
#ifdef MONITOR
	    _dapto_source_changed (infilename, 1, 1, 1, 1, 1);
#endif
	} else {
	    for (cp = fileerrmsg; *cp != '\0'; cp++)
		if (*cp == '%') {
		    switch (*(cp+1)) {
		    case 'f':
			fputs (infilename, stderr);
			break;
		    case 'p':
			fputs (argv[0], stderr);
			break;
		    case '%':
			fputc ('%', stderr);
			break;
		    default:
			break;
		    }
		    cp++;
		} else
		    fputc (*cp, stderr);
	    fputc ('\n', stderr);
	    if (fileerrmsgusage)
		clp_usage (argv[0]);
	    exit(2);
	}
    }
}
