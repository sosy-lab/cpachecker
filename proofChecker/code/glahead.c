static char RCSid[] = "$Id: glahead.c,v 1.68 2009/09/09 19:31:57 profw Exp $";
/* Copyright, 1989, The Regents of the University of Colorado */

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
#include <string.h>

#include "err.h"
#include "csm.h"
#include "source.h"
#include "obstack.h"
#include "scanops.h"
#include "tabsize.h"
#include "ScanProc.h"
#include "gla.h"

#ifdef MONITOR
#include "dapto_dapto.h"
#include "gla_dapto.h"
#endif

/* Establish a scan pointer
 *   On exit-
 *     (TokenEnd-StartLine)=column index of the character addressed
 *       by TokenEnd
 *     If the source text buffer is empty then-
 *       TokenEnd points to a null string
 *     Otherwise-
 *       TokenEnd points to a string that is guaranteed to contain
 *         a newline character
 */
#ifndef SCANPTR
#define SCANPTR { TokenEnd = TEXTSTART; StartLine = TokenEnd - 1; }
#endif

/* Set the coordinates of the current token
 *   On entry-
 *     LineNum=index of the current line in the entire source text
 *     p=index of the current column in the entire source line
 *   On exit-
 *     curpos has been updated to contain the current position as its
 *     left coordinate
 */
#ifndef SETCOORD
#ifdef MONITOR
#define SETCOORD(p) { LineOf (curpos) = LineNum; \
		      ColOf (curpos) = CumColOf (curpos) = (p); }
#else
#define SETCOORD(p) { LineOf (curpos) = LineNum; ColOf (curpos) = (p); }
#endif
#endif

#ifdef RIGHTCOORD
/* Set the coordinates of the end of the current token
 *   On entry-
 *     LineNum=index of the current line in the entire source text
 *     p=index of the current column in the entire source line
 *   On exit-
 *     curpos has been updated to contain the current position as its
 *     right coordinate
 */
#ifndef SETENDCOORD
#ifdef MONITOR
#define SETENDCOORD(p) { RLineOf (curpos) = LineNum; \
			 RColOf (curpos) = RCumColOf (curpos) = (p); }
#else
#define SETENDCOORD(p) { RLineOf (curpos) = LineNum; \
			 RColOf (curpos) = (p); }
#endif
#endif
#endif

/* Return after recognising a basic symbol.
 *   On entry-
 *     v=syntax code of the recognised symbol
 */
#ifndef RETURN
#ifdef MONITOR
#define RETURN(v) { _dapto_leave ("lexical"); return v; }
#else
#define RETURN(v) { return v; }
#endif
#endif

/* Select a continuation after recognizing a character sequence
 *   On entry-
 *     TokenEnd points to the first character not belonging to the sequence
 *     extcode=syntax code if the character sequence is a basic symbol
 *             NORETURN	if the character sequence is to be ignored
 *
 * This macro may carry out arbitrary processing, including alteration of p,
 * and on the basis of that processing may take one of three actions:
 *
 *   RETURN (v);	report a basic symbol with syntax code v
 *			TokenEnd must point to the character to be scanned
 *			  when glalex is called again
 *
 *   goto rescan;	start a new scan at the character addressed by p,
 *			  without changing the current token's coordinates
 *
 *   continue		start a new scan at the character addressed by p,
 *			  resetting the current token's coordinates
 */
#ifndef WRAPUP
#define WRAPUP { if (extcode != NORETURN) RETURN (extcode); }
#endif

#ifdef MONITOR
/* Generate monitoring event for a token.
 *   On entry-
 *     TokenEnd points to the first character not belonging to the sequence
 *     extcode=syntax code if the character sequence is a basic symbol
 *             NORETURN	if the character sequence is to be ignored
 *
 * This macro should call generate_token with the appropriate token
 * information.  It should fall through into subsequent code (WRAPUP).
 */
#ifndef WRAPUPMONITOR
#define WRAPUPMONITOR { \
  if (extcode != NORETURN) { \
    char save = *TokenEnd; \
    CONST char *name = ""; \
    *TokenEnd = '\0'; \
    if (extcode == EOFTOKEN) \
      name = "end of file"; \
    else {\
      int ind; \
      for (ind = 0; mon_nonlit_codes[ind] != -1; ind++) \
        if (mon_nonlit_codes[ind] == extcode) {\
           name = mon_token_names[ind]; \
           break; \
         }\
    } \
    _dapto_token (name, LineOf (curpos), ColOf (curpos), \
	          CumColOf (curpos), RLineOf (curpos), RColOf (curpos), \
		  RCumColOf (curpos), TokenStart, TokenEnd - TokenStart, \
		  *v, extcode); \
    *TokenEnd = save; \
  } \
}
#endif
#endif

int ResetScan = 1;	/* Initialization switch */
char *StartLine = 0;	/* Adjusted beginning of the current line */
char *TokenStart = 0;	/* First character position of the current token */
char *TokenEnd = 0;	/* First character position beyond the current token */

#define noASSERT

#ifdef noASSERT
#define assert(ex)
#else
#ifdef myASSERT
/* no need to malloc space, cause this is FATAL */
#define assert(ex) {if (!(ex)){ \
  (void)fprintf(stderr, "Assertion failed: file %s, line %d\n", \
  __FILE__, __LINE__); exit(2);
#else
#define assert(ex) ;
#endif
#endif

#include "xtables.h"

/**/
static void
#if defined(__cplusplus) || defined(__STDC__)
obstack_octgrow(ObstackP obstack, int data_char)
#else
obstack_octgrow(obstack, data_char)
ObstackP obstack; int data_char;
#endif
/* Store the octal coding of a C character into an obstack
 *   On exit-
 *     The octal coding for data_char has been added to obstack
 **/
{
  obstack_1grow(obstack, '\\');

  if (data_char >= 64) {
    obstack_1grow(obstack, '0' + data_char / 64);
    data_char %= 8;
  } else obstack_1grow(obstack, '0');

  if (data_char >= 8) {
    obstack_1grow(obstack, '0' + data_char / 8);
    data_char %= 8;
  } else obstack_1grow(obstack, '0');

  obstack_1grow(obstack, '0' + data_char);
}

/**/
static void
#if defined(__cplusplus) || defined(__STDC__)
obstack_cchgrow(ObstackP obstack, int data_char)
#else
obstack_cchgrow(obstack, data_char)
ObstackP obstack; int data_char;
#endif
/* Store a C character into an obstack
 *   On exit-
 *     The C form of data_char has been added to obstack
 **/
{
  if (data_char >= '\177') obstack_octgrow(obstack, data_char);
  else if (data_char == '\\') obstack_grow(obstack, "\\\\", 2);
  else if (data_char == '"') obstack_grow(obstack, "\\\"", 2);
  else if (data_char >= ' ') obstack_1grow(obstack, data_char);
  else switch (data_char) {
  case '\n': obstack_grow(obstack, "\\n", 2); break;
  case '\t': obstack_grow(obstack, "\\t", 2); break;
  case '\b': obstack_grow(obstack, "\\b", 2); break;
  case '\r': obstack_grow(obstack, "\\r", 2); break;
  case '\f': obstack_grow(obstack, "\\f", 2); break;
  default: obstack_octgrow(obstack, data_char); }
}

/***/
void
#if defined(__cplusplus) || defined(__STDC__)
lexerr(char *start, int length, int *code, int *intrinsic)
#else
lexerr(start, length, code, intrinsic)
char *start; int length, *code, *intrinsic;
#endif
/* Report a token error
 *   On entry-
 *     start points to the first character of the error sequence
 *     length=length of the error sequence
 *     code points to a location containing the initial classification
 *     intrinsic points to a location to receive the value
 *   On exit-
 *     An error report has been issued
 ***/
{ obstack_1grow(Csm_obstk, '\'');
  while (length-- > 0) obstack_cchgrow(Csm_obstk, *start++);
  message(
    ERROR,
    (char *)obstack_copy0(Csm_obstk, "' is not a token", 16),
    0,
    &curpos);
}


#ifdef GLAPRINTTOKENS

int
#if defined(__cplusplus) || defined(__STDC__)
debugglalex(int *);
#else
debugglalex();
#endif

int
#if defined(__cplusplus) || defined(__STDC__)
glalex (int *v)
#else
glalex (v)
int *v;		/* pointer to storage for intrinsic value */
#endif
{
  int code;
  char tmp;
  static int init = 0;

#ifdef MONITOR
  _dapto_enter ("lexical");
#endif

  code = debugglalex (v);	/* first do the work */

  if (GlaPrintTokens) {	/* print info ? */
    if (init == 0) {
      init++;
      fprintf (stderr, "(line,col) code,intrinsic\tsrc-matched  TOK\n");
    }
    tmp = *TokenEnd;	/* make a string for printf */
    *TokenEnd = '\0';
    fprintf (stderr, "(%d,%d)\t%d,%d\t\"%s\"\t",
       curpos.line, curpos.col,code, *v, TokenStart);
    
    /* Is code within limits? */
    if (code >= 0 &&
        code < sizeof (TokenStrings) / sizeof (TokenStrings[0]))
      fprintf (stderr, "%s\n",
         TokenStrings[code] == NULL ? "NULL"
         : TokenStrings[code]);
    else /* no */
      fprintf (stderr, "non-compact token code\n");

    *TokenEnd = tmp;
  }

#ifdef MONITOR
  _dapto_leave ("lexical");
#endif

  return(code);
}

int
#if defined(__cplusplus) || defined(__STDC__)
debugglalex(int *v)
#else
debugglalex(v)
int *v;		/* pointer to storage for intrinsic value */
#endif

#else
int
#if defined(__cplusplus) || defined(__STDC__)
glalex(int *v)
#else
glalex(v)
int *v;		/* pointer to storage for intrinsic value */
#endif
#endif
{
#if defined(__cplusplus) || defined(__STDC__)
  char *(*scan)(char *, int)=NULL;
  void  (*proc)(const char *, int, int *, int *)=NULL;
#else
  char *(*scan)()=NULL;
  void  (*proc)()=NULL;
#endif
  register unsigned char c;	/* hold current char */
  int extcode;			/* external token repr */
#ifdef MONITOR
  int retcode;
#endif

  /* this holds the base in a register */
  register unsigned char *scanTbl = ScanTbl; 

  register char *p;		/* most current working pointer */

#ifdef MONITOR
  _dapto_enter ("lexical");
#endif

  /* continue/start a new token */
  for(;;) {
    if (ResetScan) { ResetScan = 0; SCANPTR; }
    p = TokenEnd;	/*ASSERT TokenEnd points to first char in buffer */
    SETCOORD(p - StartLine);

rescan:
    TokenStart = p;

      /****************************/
      /* generated code goes here */
      /****************************/
#include "xcode.h"
      
      /* xcode.h has the entire switch statement! */
      
  fallback:
    if (TokenEnd == TokenStart) {
      /* never passed thru final state */
      obstack_grow(Csm_obstk, "char '", 6);
      obstack_cchgrow(Csm_obstk, *TokenStart);
      message(
        ERROR,
        (char *)obstack_copy0(Csm_obstk, "' is not a complete token", 25),
        0,
        &curpos);
      p = TokenEnd = TokenStart + 1;
      continue;
    }
    
    /* TokenStart may not exceed TokenEnd */
    assert (TokenEnd >= TokenStart);
    /* we now know that End > Start */
    
    assert (*p != '\0');
    /* we must make progress */
    assert (p > TokenStart);
    /* TokenEnd must never exceed p */
    assert (p >= TokenEnd);
    
    /*
     * At this point we know we have found a token, but
     * need to handle the case where, while looking for
     * a long token, we passed over one or more shorter
     * tokens - the long token was never found.
     * Must fallback to shorter final state 
     */
    if (TokenEnd < p) p = TokenEnd;
    
    if (scan != NULL)
      TokenEnd = p = (*scan) (TokenStart, TokenEnd - TokenStart);
    
    if (proc != NULL)
      (*proc) (TokenStart, TokenEnd - TokenStart, &extcode, v);

done:
#ifdef RIGHTCOORD
    SETENDCOORD(TokenEnd - StartLine);
#endif
#ifdef MONITOR
    WRAPUPMONITOR
#endif
    WRAPUP
  } /* end for ever */
}
