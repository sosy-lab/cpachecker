static char RCSid[] = "$Id: err.c,v 1.57 2009/08/05 22:06:05 profw Exp $";
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
#include <string.h>
#include <fcntl.h>

#if defined(unix) || defined (_unix) || defined(__cplusplus)
#include <unistd.h>
#endif

#if defined(_WIN32) || defined(MSDOS) || defined(_MSDOS)
#include <io.h> 
#endif

#include "err.h"
#include "source.h"
#ifdef MONITOR
#include "dapto_dapto.h"
#include "err_dapto.h"
#endif

	/* Variables exported by the Error Module */

int ErrorCount[] = {		/* Counts at each severity level */
  0, 0, 0, 0
  };
int LineNum = 1;		/* Index of the current line
				   in the total source text */

POSITION NoCoord = { 0 };       /* The NULL coordinate */

POSITION curpos;		/* Position variable for general use */

static CONST char *key[] = {"NOTE", "WARNING", "ERROR", "DEADLY"};

struct msg {
  int severity;
  POSITION loc;
  int grammar;
  CONST char *Msgtext;
  struct msg *forward, *back;
};


static struct msg reports = {   /* Error report list */
  DEADLY, {0}, 0, "", &reports, &reports};

static struct msg emergency;    /* In case malloc fails */

static int ImmediateOutput = 1;	/* 1 if immediate error output required */
static int GrammarLine = 1;	/* 1 to print AG line number */
static int ErrorLimit = 1;	/* 1 to abort after too many errors */

void
#ifdef PROTO_OK
ErrorInit(int ImmOut, int AGout, int ErrLimit)
#else
ErrorInit(ImmOut, AGout, ErrLimit) int ImmOut, AGout, ErrLimit;
#endif
/* Initialize the error module
 *    On entry-
 *       ImmOut=1 if immediate error output required
 *       AGout=1 to print AG line number on error reports
 *       ErrLimit=1 to limit the number of errors reported
 ***/
{
#ifdef MONITOR
  _dapto_enter ("message");
#endif

  ImmediateOutput = ImmOut;
  GrammarLine = AGout; 
  ErrorLimit = ErrLimit;

  reports.severity = DEADLY;
  reports.loc.line = reports.loc.col = 0;
  reports.grammar = 0; reports.Msgtext = "";
  reports.forward = reports.back = &reports;

#ifdef MONITOR
  _dapto_leave ("message");
#endif
}

int
#ifdef PROTO_OK
earlier(POSITION *p, POSITION *q)
#else
earlier(p,q) POSITION *p, *q;
#endif
/* Check relative position
 *    On exit-
 *       earlier != 0 if p defines a position in the source Msgtext that
 *          preceeds the position defined by q
 ***/
{
  if (p->line != q->line) return(p->line < q->line);
  return(p->col < q->col);
}


void
#ifdef PROTO_OK
lisedit(CONST char *name, FILE *stream, int cutoff, int erronly)
#else
lisedit(name, stream, cutoff, erronly)
CONST char *name; FILE *stream; int cutoff, erronly;
#endif
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
{
  register char *p;
  int fd;
  struct msg *r;
 
#ifdef MONITOR
  _dapto_enter ("message");
#endif

  /* Establish the following invariant:
   *   if no reports remain to be output then r==&reports
   *   else r addresses the earliest (in the text) report to be output
   */
  for (r = reports.forward;
       r != &reports && r->severity < cutoff;
       r = r->forward) ;

  fd = -1;
  if (name != NULL && *name != '\0' && (fd = open(name, O_RDONLY)) < 0)
    perror(name);

  if (fd < 0) {
    while (r != &reports && r->severity >= cutoff) {
      (void)fprintf(stream, "line %d:%d %s: %s\n",
        r->loc.line, r->loc.col, key[r->severity], r->Msgtext);
      r = r->forward;
    }
  } else {
    initBuf(name, fd);
    p = TEXTSTART; LineNum = 1;
  
    while (r != &reports && r->loc.line == 0) {
      if (r->severity >= cutoff){
        (void)fprintf(stream, "*** %s: %s\n", key[r->severity], r->Msgtext);
      }
      do r = r->forward; while (r != &reports && r->severity < cutoff);
    }
    while (r != &reports || (!erronly && *p != 0)) {
      if (r != &reports && LineNum > r->loc.line) {
  			/* Output reports for the last line printed */
        char buf[BUFSIZ];
        int l, s;
   
        (void)sprintf(buf, "*** %s: %s", key[r->severity], r->Msgtext);
        l = strlen(buf);
        s = r->loc.col - 1 + (erronly?8:0);
        if (l > s) {
          while (s--) (void)putc(' ', stream);
          (void)fprintf(stream, "^\n%s\n", buf);
        } else {
          (void)fprintf(stream, "%s", buf);
          while (l < (s--)) (void)putc('-', stream);
          (void)fprintf(stream, "^\n");
        }
        do r = r->forward; while (r != &reports && r->severity < cutoff);
      } else { /* Print up through the next line with a report */
        register char c;
        char *StartLine = p;
  
        while ((c = *p++) && c != '\n' && c != '\r') ;
        if (c == '\r' && *p == '\n') c = *p++;
        if (c == '\n' || c == '\r') {
          if (!erronly || LineNum == r->loc.line) {
            if (erronly) (void)fprintf(stream, "%6d |", LineNum);
            (void)fwrite(StartLine, p-StartLine, 1, stream);
          }
          if (*p == 0) { refillBuf(p); p = TEXTSTART; }
        } else /* c == 0 */ {
          if (erronly) (void)fprintf(stream, "%6d |", LineNum);
          (void)fputs("(End-of-file)\n", stream);
          p--;
        }
        LineNum++;
      }
    }
    (void)close(finlBuf());
  }

#ifdef MONITOR
  _dapto_leave ("message");
#endif
}  


void
#ifdef PROTO_OK
message(int severity, CONST char *Msgtext, int grammar, POSITION *source)
#else
message(severity, Msgtext, grammar, source)
int severity; CONST char *Msgtext; int grammar; POSITION *source;
#endif
/* Report an error
 *    On entry-
 *      severity=error severity
 *      Msgtext=message text
 *      grammar=identification of the test that failed
 *      source=source coordinates at which the error was detected
 ***/
{ CONST char *SrcFile = SrcBuffer ? SRCFILE : ".";
  int fail = 0;
  struct msg *r, *c;

#ifdef MONITOR
  _dapto_enter ("message");
#endif

  if (severity < NOTE || severity > DEADLY) {
    (void)fprintf(stderr, "Invalid severity code %d for \"%s\"\n",
      severity, Msgtext);
    severity = DEADLY;
    }

  if (source == (POSITION *)0) source = &NoCoord;

#ifdef MONITOR
  _dapto_message (key[severity], Msgtext, source->line, source->col);
#endif

  if (ImmediateOutput) {
    (void)fprintf(stderr, "\"%s\", line %d:%d %s: %s",
        SrcFile, source->line,source->col, key[severity], Msgtext);
    if (grammar>0 && GrammarLine) (void)fprintf(stderr," AG=%d\n", grammar);
    else (void)putc('\n', stderr);
    (void)fflush(stderr);
  }

  ErrorCount[severity]++;
  
  if ((r = (struct msg *)malloc(sizeof(struct msg))) == (struct msg *)0) {
    r = &emergency;
    (void)fprintf(stderr, "No storage for error report at");
    fail = 1;
  }
  r->loc = *source;
  r->severity = severity;
  r->Msgtext = Msgtext;
  r->grammar = grammar;

  c = reports.back; while (earlier(&r->loc,&c->loc)) c = c->back;
  r->forward = c->forward; c->forward = r;
  r->back = c; (r->forward)->back = r;

  
  if(ErrorLimit && ErrorCount[ERROR] > LineNum/20 +10) {
    (void)fprintf(stderr, "\"%s\", line %d:%d %s: %s\n",
      SrcFile, source->line, source->col, key[DEADLY], "Too many ERRORs");
    fail = 1;
  }
  if (severity == DEADLY || fail ) {
    if (!ImmediateOutput) {
      if (SrcBuffer && SrcBuffer->fd != 0) {
        SrcBufPtr temp = SrcBuffer;
        lisedit(temp->name, stderr, NOTE, 1);
        SrcBuffer = temp;
      } else lisedit(NULL, stderr, NOTE, 1);
    }
#ifdef MONITOR
    _dapto_leave ("message");
#endif
    exit(1);
  }

#ifdef MONITOR
  _dapto_leave ("message");
#endif
}
