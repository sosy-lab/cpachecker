static char RCSid[] = "$Id: csm.c,v 3.8 1999/03/11 15:18:45 mjung Exp $";
/* Copyright 1989, The Regents of the University of Colorado */

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
#include <stdlib.h>
#include "csm.h"
#include "csmtbl.h"
#ifdef MONITOR
#include "dapto_dapto.h"
#include "csm_dapto.h"
#endif
#ifndef NORESTORE
#include "obsave.h"
#endif
#ifndef NOPRINT

/***/
void
#ifdef PROTO_OK
prtstcon(FILE *d, const char *p)
#else
prtstcon(d, p) FILE *d; char *p;
#endif
/* Print a sequence of characters as a string constant without quotes
 *    On exit-
 *       The string pointed to by p has been added to the current
 *          line of d as a string constant without quotes
 ***/
{
  char c;

#ifdef MONITOR
  _dapto_enter ("string");
#endif

  while ((c = *p++ & 0377) != '\0') {
    if (c >= '\177') fprintf(d, "\\%3o", c);
    else if (c == '\\') fprintf(d, "\\\\");
    else if (c == '"') fprintf(d, "\\\"");
    else if (c >= ' ') (void)putc(c, d);
    else switch (c) {
    case '\n': fprintf(d, "\\n"); break;
    case '\t': fprintf(d, "\\t"); break;
    case '\b': fprintf(d, "\\b"); break;
    case '\r': fprintf(d, "\\r"); break;
    case '\f': fprintf(d, "\\f"); break;
    default: fprintf(d, "\\%03o", c); }
  }

#ifdef MONITOR
  _dapto_leave ("string");
#endif

  return;
}

/***/
char *
#ifdef PROTO_OK
StringTable(int i)
#else
StringTable(i) int i;
#endif
/* Obtain a string from the string table
 *   On entry-
 *     i indexes the desired string
 *   On exit-
 *     StringTable points to the indexed string
 ***/
{ return strng[i]; }

#ifdef SAVE
/***/
void
#ifdef PROTO_OK
savestr(FILE *d)
#else
savestr(d) FILE *d;
#endif
/* Save the current string table state
 *    On exit-
 *       File d is a symbolic encoding of the current state, suitable
 *          for inclusion in the string table module
 ***/
{
  char *p;
  int k, TotalCh;

#ifdef MONITOR
  _dapto_enter ("string");
#endif

  TotalCh = 0;
  for (k = 0; k < numstr; k++) TotalCh = TotalCh + strlen(strng[k]) + 1;

  fprintf(d, "\t/* This is an initialized obstack */\n\n");
  fprintf(d, "static struct {char *l; void *p; char c[%d];} csm_data = {\n\
\t&(csm_data.c[%d]),\n\
\t0,\n\
\"", TotalCh, TotalCh);

  for (k = 0; k < numstr; k++) {
    prtstcon(d, strng[k]);
    if (k < numstr-1) fprintf(d, "\\0\\\n");
  }
  fprintf(d, "\"};\n\n");

  fprintf(d, "struct obstack csm_obstack =\n\
\tobstack_known_chunk(&csm_data,\n\
\t&(csm_data.c[%d]), &(csm_data.c[%d]), &(csm_data.c[%d]),\n\
\t4096, 1);\n\n", TotalCh, TotalCh, TotalCh);

  fprintf(d, "\t/* This is an initialized obstack */\n\n");
  fprintf(d, "static struct {char **l; void *p; char *c[%d];} csm_indx = {\n\
\t&(csm_indx.c[%d]),\n\
\t0,{\n", numstr, numstr);

  TotalCh = 0;
  for (k = 0; k < numstr; k++) {
    (void)fprintf(d, "\t&(csm_data.c[%d])", TotalCh);
    TotalCh = TotalCh + strlen(strng[k]) + 1;
    if (k < numstr-1) fprintf(d, ",\n");
  }

  fprintf(d, "}};\n\n");

  fprintf(d, "struct csmalign {char ___x; char *___d;};\n\
static struct obstack csm_indx_obstk =\n\
\tobstack_known_chunk(&csm_indx, &(csm_indx.c[0]),\n\
\t&(csm_indx.c[%d]), &(csm_indx.c[%d]), 4096,\n\
\t((PTR_INT_TYPE) ((char *) &((struct csmalign *) 0)->___d - (char *) 0)));\n\n",
    numstr, numstr);

  fprintf(d, "char **strng = csm_indx.c;\n\
int numstr = %d;\n\n", numstr);

#ifdef MONITOR
  _dapto_leave ("string");
#endif
}
#endif

/***/
void
#ifdef PROTO_OK
dmpstr(FILE *d)
#else
dmpstr(d) FILE *d;
#endif
/* Dump the string table
 *    On exit-
 *       The string table contents have been written to d
 ***/
{
  int i;

#ifdef MONITOR
  _dapto_enter ("string");
#endif

  if (numstr == 0) fprintf(d,"\n String Table is empty");
  else {
    fprintf(d,"\n String Table Contents-\n");
    for (i = 0; i < numstr; i++) {
      fprintf(d, " \"");
      (void) prtstcon(d, strng[i]);
      fprintf(d, "\"\n");
    }
  }
  fprintf(d,"\n\n");

#ifdef MONITOR
  _dapto_leave ("string");
#endif
}

#endif

char *CsmStrPtr = (char *)0;	/* String stored in Csm_obstk */

/***/
int
#ifdef PROTO_OK
stostr(const char *c, int l)
#else
stostr(c, l) char *c; int l;
#endif
/* Store a string in the string table
 *    On entry-
 *       c points to the string to be stored
 *       l>0 = length of the string to be stored
 *    On exit-
 *       stostr=string table index of the stored string
 ***/
{
#ifdef MONITOR
  _dapto_enter ("string");
#endif

  obstack_blank(&csm_indx_obstk, sizeof(char *));
  strng = (char **)obstack_base(&csm_indx_obstk);

  strng[numstr] =
    c == CsmStrPtr ? CsmStrPtr : (char *)obstack_copy0(Csm_obstk, c, l);

#ifdef MONITOR
  {
    _dapto_string_stored (numstr, strng[numstr]);
    _dapto_leave ("string");
    return (numstr++);
  }
#else
  return (numstr++);
#endif
}

#ifndef NORESTORE

static struct csmdata {
    void *characterstrings;
    size_t indexsize;
    int numstr;
} savedata;

void *SaveModuleCsm()
{
    savedata.characterstrings = obstack_alloc(&csm_obstack, 0);
    savedata.indexsize = obstack_object_size(&csm_indx_obstk);
    savedata.numstr = numstr;
    
    return SaveData(&savedata, sizeof(savedata));
}

void
#ifdef PROTO_OK
RestoreModuleCsm(void *base)
#else
RestoreModuleCsm(base) void *base;
#endif
{
    RestoreData(base);
    
    obstack_free(&csm_obstack, savedata.characterstrings);
    numstr = savedata.numstr;
    obstack_next_free(&csm_indx_obstk) = 
	(char *)obstack_base(&csm_indx_obstk) + savedata.indexsize;
    
    strng = (char **)obstack_base(&csm_indx_obstk);
}

#endif
