#ifndef GSDESCR_H
#define GSDESCR_H
/* $Id: gsdescr.h,v 1.8 1997/09/04 18:21:17 waite Exp $ */
/* Scanner interface for a generated parser
   Copyright 1996, The Regents of the University of Colorado */

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
#include "parsops.h"

typedef int TERMINALSYMBOL;
typedef	int ATTRTYPE;

typedef struct
  { POSITION        Pos;
    TERMINALSYMBOL  SyntaxCode;
    ATTRTYPE	    Attr;
  } GRUNDSYMBOLDESKRIPTOR;

#define T_CODE(tok)       ((tok).SyntaxCode)
#define T_POS(tok)        ((tok).Pos)
#define T_ATTR(tok)       ((tok).Attr)

/* Scanner interface: read next token from input stream */
#ifndef SCANNER
#define SCANNER glalex
#endif

#define	GET_TOKEN(tok) \
{ T_CODE(tok) = SCANNER( (int *)(&(T_ATTR(tok))) ); T_POS(tok) = curpos; }

#endif
