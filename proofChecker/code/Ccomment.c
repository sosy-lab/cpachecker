static char RCSid[] = "$Id: Ccomment.c,v 1.18 2004/08/01 01:44:25 profw Exp $";
/* Copyright 1996, The Regents of the University of Colorado */

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
#include "gla.h"
#include "source.h"
#include "tabsize.h"
#include "ScanProc.h"

/*
**  Returns pointer to first character beyond comment.
*/

char *
#if defined(__cplusplus) || defined(__STDC__)
auxCComment(char *start, int length)
#else
auxCComment(start, length) char * start; int length;
#endif
/* Scan a C comment after the opening delimiter
 *   On entry
 *     start points to the opening delimiter
 *     len=length of the opening delimiter
 *   On exit-
 *     Ctext points to the character position following the
 *       closing delimiter
 ***/
{ register char c;
  register char *p = start + length;	/* first char not yet processed */

  for (;;) {
    if (*p == '\0') {
      int current = p - start;
      TokenStart = start = auxNUL(start, current);
      p = start + current;
      StartLine = p - 1;
      if (*p == '\0') {
        message(ERROR,"file ends inside this comment",0,&curpos);
        return p;
      }
    }
    c = *p++;

    if (c == '\r' && *p == '\n') c = *p++;
    if (c == '\n' || c == '\r') { LineNum++; StartLine = p - 1; }
    else if (c == '\t') StartLine -= TABSIZE(p - StartLine);
    else if (c == '*' && *p == '/') return p + 1;
  }
}
