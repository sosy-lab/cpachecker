#ifndef CLPINTF_DEF
#define CLPINTF_DEF

/*
 * $Id: clpintf.h,v 1.9 2009/08/05 19:42:46 profw Exp $
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
 * ParseCommandLine
 *
 * On entry-
 *    argv contains the arguments which need to be parsed
 *    argc contains the number of arguments
 * On exit-
 *    The command line has been parsed according the user's clp specs.
 *    Also, the source module has been initialized with the first buffer of
 *    the processor's input.  If the input file cannot be opened, an error
 *    message is issued and the program is exited.
 */

extern void ParseCommandLine ELI_ARG((int argc, char *argv[]));

/*
 * SetFileErr
 *
 * On entry-
 *    msg contains a nul-terminated string
 *    usage contains a boolean value
 * On exit-
 *    Error message to be used when file cannot be opened has been
 *    changed to msg.  If msg contains a %s then it will be replaced
 *    on output by the name of the file that could not be opened.
 *    If usage is true then when an error message is printed, the usage
 *    message is also printed.
 */

extern void SetFileErr ELI_ARG((const char *msg, int usage));

#endif
