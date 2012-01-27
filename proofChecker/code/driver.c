static char RCSid[] = "$Id: driver.c,v 1.23 2008/06/12 02:13:01 profw Exp $";
/* Driver for general text processing programs */
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

#include <stdio.h>
#include "err.h"
#include "HEAD.h"
#ifdef MONITOR
#include "dapto.h"
#include "dapto_dapto.h"
#endif

#ifdef ELI_ARG
#undef ELI_ARG
#endif
#if defined(__cplusplus) || defined(__STDC__)
#define ELI_ARG(x) x
#else
#define ELI_ARG(x) ()
#endif

extern void ParseCommandLine ELI_ARG((int, char *[]));
extern void Zerteiler ELI_ARG((void));
extern void ATTREVAL ELI_ARG((void));

/* The following is just a dummy function used in the Eli derivation to */
/* make sure that the main program gets included. */
int
___Eli_Main()
{
  return 0;
}

int
#if defined(__cplusplus) || defined(__STDC__)
main(int argc , char *argv[])
#else
main(argc, argv) int argc; char *argv[];
#endif
{
#ifdef MONITOR
  _dap_init (argv[0]);
  _dapto_enter ("driver");
#endif

  ParseCommandLine(argc, argv);

#include "INIT.h"

  Zerteiler();

#ifdef STOPAFTERBADPARSE
  if (ErrorCount[ERROR] == 0)
#endif
  ATTREVAL();

#include "FINL.h"

#ifdef MONITOR
  _dapto_leave ("driver");
#endif
  return (ErrorCount[ERROR] > 0);
}
