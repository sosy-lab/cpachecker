static char RCSid[] = "$Id: idn.c,v 1.28 2009/09/09 19:31:57 profw Exp $";
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
#include <ctype.h>
#include "err.h"
#include "idn.h"
#include "csm.h"
#include "obstack.h"
#ifndef NORESTORE
#include "obsave.h"
#endif

	/* Types local to the identifier table */

typedef struct chainelt {	/* Lookup mechanism */
	struct chainelt *nxt;	   /* Chain link */
	int len;		   /* Length */
	int typ;		   /* Terminal */
	int dat;		   /* Corresponding identifier */
} CHAIN;

#include "idntbl.h"


#ifndef NOPRINT

/***/
void
#ifdef PROTO_OK
prtidnv(FILE *d, int i)
#else
prtidnv(d, i) FILE *d; int i;
#endif
/* Print an identifier
 *    On exit-
 *       The identifier encoded by i has been added to the current
 *          line of d.
 ***/
{
	fprintf(d, " \"");
	(void) prtstcon(d, StringTable(i));
	(void) putc('"', d);
}

#ifdef SAVE
/**/
void
#ifdef PROTO_OK
savchain(FILE *d, CHAIN *p, int k, int i, int which)
#else
savchain(d, p, k, i, which) FILE *d; CHAIN *p; int k, i, which;
#endif
/* Print a chain of hash entries
 *    On entry-
 *       the chain to be printed is attached to state.hash[k]
 *       i is the index of the first block of the chain pointed to by p
 *    On exit-
 *       if which == 0, a component of the structure definition is printed
 *       else the contents of the structure is printed
 *       on file d
 **/
{
	if (p != (CHAIN *)0) {
		savchain(d, p->nxt, k, i+1, which);
		if (which == 0)
			fprintf(d, "\tCHAIN h%dl%d;\n", k, i);
		else {
			fprintf(d, "{");
			if (p->nxt == (CHAIN *)0) fprintf(d, "NULL");
			else fprintf(d, "&state.h%dl%d", k, i+1);
			fprintf(d, ",%d,%d,%d},\n", p->len, p->typ, p->dat);
		}
	}
}

/***/
void
#ifdef PROTO_OK
saveidn(FILE *d)
#else
saveidn(d) FILE *d;
#endif
/* Save the current identifier table state
 *    On exit-
 *       File d is a symbolic encoding of the current state, suitable
 *          for inclusion in the identifier table module
 ***/
{
	int k;

	fprintf(d, "#define HTSIZE %d\n", HTSIZE);
	fprintf(d, "static struct idntbl_state {\n");
	for (k = 0; k < HTSIZE; k++) savchain(d, state.hash[k], k, 0, 0);
        fprintf(d, "\n\tCHAIN *hash[HTSIZE];\n} state = {\n");
	for (k = 0; k < HTSIZE; k++) savchain(d, state.hash[k], k, 0, 1);

	fprintf(d, "\n{\n");
	for (k = 0; k < HTSIZE; k++) {
		if (state.hash[k] == (CHAIN *)0) fprintf(d, "\tNULL");
		else fprintf(d, "\t&state.h%dl0", k);
		if (k < HTSIZE-1) fprintf(d, ",\n");
	}
	fprintf(d, "}};\n\n");
}
#endif

/***/
void
#ifdef PROTO_OK
dmpidn(FILE *d)
#else
dmpidn(d) FILE *d;
#endif
/* Dump the identifier table
 *    On exit-
 *       The entire identifier table has been written to d
 ***/
{
	int i, j;
	CHAIN *p;

	fprintf(d,"\n Identifier table contents-\n");
	j = 0;
	for (i = 0; i < HTSIZE; i++) {
		p = state.hash[i];
		if (p) {
			if (j == i-1)
				fprintf(d,"   Bin %d\n",j);
			else if (j < i)
				fprintf(d,"   Bins %d-%d empty\n",j,i-1);
			j = i+1;
			fprintf(d,"   Bin %d\n",i);
			do {
				fprintf(d, "Id=%d, Term=%d, Str=%d:\"",
					p->dat, p->typ, p->len);
				(void) prtstcon(d, StringTable(p->dat));
				fprintf(d, "\"\n");
				p = p->nxt;
			} while (p);
		}
	}
	if (j == HTSIZE-1)
		fprintf(d,"   Bin %d\n",j);
	else if (j < HTSIZE-1)
		fprintf(d,"   Bins %d-%d empty\n", j, HTSIZE-1);
	(void)putc('\n',d);
}

#endif

static Obstack idn_obstack = obstack_empty_chunk(4096, OBSTACK_PTR_ALIGN);
	
/***/
void
#ifdef PROTO_OK
mkidn(const char *c, int l, int *t, int *s)
#else
mkidn(c, l, t, s) char *c; int l, *t; int *s;
#endif
/* Obtain the internal coding of an identifier or keyword
 *    On entry-
 *       c points to the identifier or keyword
 *       l=length of the identifier or keyword
 *       t points to a location containing the initial terminal code
 *    If the identifier or keyword has appeared previously then on exit-
 *       t has been set to the terminal code given on its first appearance
 *       s has been set to the internal coding set on its first appearance
 *    Otherwise on exit-
 *       t remains unchanged
 *       s has been set to a new internal coding
 ***/
{
	register CHAIN *ent;
	register int test;
#ifndef NOFOLD
	char fold[BUFSIZ];

	if (dofold) {
		register char x;
		register char *p = fold;
		register CONST char *q = c;
		register int i = l;

		while (i--) *p++ = islower(x = toascii(*q++))?toupper(x):x;
		c = fold;
	}
#endif

        /* check for null string */
        if (l == 0) {
                *s = 0;
                return;
        }
        /* check for illegal values */
        if (l < 0) {
                message(DEADLY, "Negative length in mkidn", 0, &curpos);
        }
        /* string length l >= 1 */
	test = 1;
	if (l == 1) {
		ent = (CHAIN *) &state.hash[(int)(*c)];
		if (ent->nxt != (CHAIN *)0) {
			ent = ent->nxt;
			test = 1 - ent->len;
		}
	} else {
		{
			register CONST char *cr = c;
			register int key = 0;
			register int lr = l;

			do key += *cr++; while (--lr);
			ent = (CHAIN *) &state.hash[key & 0xFF];
		}

		do {
			if (ent->nxt == (CHAIN *)0) break;
			ent = ent->nxt;
			if ((test = l - ent->len) == 0) {
				register CONST char *cr = c;
				register char *p = StringTable(ent->dat);
				register int lr = l;

				do ; while (*cr++ == *p++ && --lr);
				test = cr[-1] - p[-1];
			}
		} while (test > 0);
	}
	if (test != 0) {
		CHAIN *temp;

		temp = (CHAIN *) obstack_alloc(&idn_obstack, sizeof(CHAIN));
		temp->nxt = ent->nxt; ent->nxt = temp;
		if (test < 0) {
			temp->len = ent->len;
			temp->typ = ent->typ;
			temp->dat = ent->dat;
		} else ent = temp;
		ent->len = l; ent->typ = *t; ent->dat = stostr(c,l);
	} else {
	  if (ent->typ != 0) *t = ent->typ;
	  if (c == CsmStrPtr && c != StringTable(ent->dat))
	    obstack_free(Csm_obstk, c);
	}
	*s = ent->dat;
}

#ifndef NORESTORE

static struct idndata {
   void *idn_obstack;
   void *idn_state;
} savedata;

void *SaveModuleIdn()
{
	void *mark = obstack_alloc(&idn_obstack, 1);
	
	savedata.idn_obstack = SaveObstack(&idn_obstack, mark);
	savedata.idn_state = SaveData(&state, sizeof(state));

	return SaveData(&savedata, sizeof(savedata));
}

void
#ifdef PROTO_OK
RestoreModuleIdn(void *base)
#else
RestoreModuleIdn(base) void *base;
#endif
{
	RestoreData(base);

	RestoreObstack(&idn_obstack, savedata.idn_obstack);
	RestoreData(savedata.idn_state);
}

#endif

