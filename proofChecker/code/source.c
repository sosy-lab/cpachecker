static char RCSid[] = "$Id: source.c,v 2.12 2008/02/29 11:46:59 asloane Exp $";
/* Copyright 1994, The Regents of the University of Colorado */

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
#include <assert.h>
#if defined(unix) || defined (_unix) || defined(__cplusplus)
#include <unistd.h>
#endif

#if defined(_WIN32) || defined(MSDOS) || defined(_MSDOS)
#include <io.h> 
#endif

#include "source.h"
#ifdef MONITOR
#include "dapto_dapto.h"
#endif

#define READSIZ 4096	/* primitive I/O in this size bytes */
#define MAXLINE 256	/* Initial guess at maximum line length */
#define NUL '\0'	/* the ASCII NUL character */

/* Source text buffering.  (See Waite, W. M. "The Cost of Lexical Analysis"
 * Software - Practice and Experience, 16 (May, 1986) 473-488.)
 * The input file is assumed to be ASCII, with no embedded NULs.
 *
 * Designed to be fast, and guarantee that full lines are always in memory.
 * The idea is to read text into a buffer and then to find the last newline
 * by scanning in reverse from the end of the buffer.  The character
 * following that newline is saved and replaced by NUL to mark the end of
 * the usable text.  (An extra position is reserved in the buffer in case
 * the last newline is the last character read.)
 *
 * If there is no newline in the buffer after it has been filled, then
 * space is reallocated for a larger buffer and another block of input is
 * read and stored immediately after the previous text.  If the end of the
 * file is reached without reading a newline, then a newline is added at
 * the end of the text.
 *
 * Client routines of the source module can assume that a complete line,
 * with its terminating newline, is in the buffer.  If the character
 * following the terminating newline is NUL, then refillBuf must be called
 * to obtain additional information.  (If the character following the
 * terminating newline is not NUL, then the module guarantees that the next
 * line, with its associated newline, is already in the buffer.)
 *
 * When refillBuf is called, the buffer generally contains the head of a
 * partial line that follows the last complete line.  This partial line
 * must be concatenated with the new text read, because the new text is the
 * continuation of the partial line.  The buffer is therefore divided into
 * two parts:
 */
#define READBUF (SrcBuffer->memblock+SrcBuffer->maxline)
/*
 *   memblock		 READBUF
 *	 |		 |
 *	|---------------|---------------------------------------------------|
 *	| <--maxline-->	|  <--------readsiz ----------------------------->  |
 *	|	^	|				^	^	    |
 *	|TEXTSTART	|			     charend  READBUF+count |
 *	|-------------------------------------------------------------------|
 *
 * The left part of the buffer, of size maxline, is the "partial line area"
 * and the right part, of size readsiz, is the "read area".  Initially,
 * text is read into the read area.  The number of characters read is saved
 * as the value of "count", and "charend" is set to point to the NUL that
 * is stored after the newline terminating the last full line.  When the
 * buffer is refilled, the partial line is first copied to the partial line
 * area so that its last character is in the last character position of
 * that area.  Then text is read, starting at the beginning of the read
 * area.  This strategy performs the concatenation as cheaply as possible.
 * 
 * The basic strategy outlined above must be modified slightly because the
 * interface to refillBuf permits it to be called with a pointer to ANY
 * character position in the buffer.  The intent of such a call is to add
 * new text (if there is any in the file) to the string that begins at the
 * specified character and includes the partial line at the end of the
 * buffer.  This string may be very long, and may actually begin in the
 * partial line area.
 *
 * The modified strategy is to move the existing contents of the buffer to
 * the left if necessary to guarantee that it begins in the partial line
 * area, and to read text into the first unoccupied character position.
 */

SrcBufPtr SrcBuffer = (SrcBufPtr)0;	/* Current source text buffer */

#ifdef MONITOR
#define Exception(e) perror(e); _dapto_leave("source"); exit(1);
#else
#define Exception(e) perror(e); exit(1);
#endif

static int
#if defined(__cplusplus) || defined(__STDC__)
FullLine(char *p)
#else
FullLine(p)
char *p;
#endif
/* Test whether new information read contains a newline
 *   On entry-
 *     memblock < p < READBUF+count
 *   If [p..READBUF+count-1] contains a newline then on exit-
 *     FullLine == 1
 *     p < charend <= READBUF+count
 *     charend[-1] == '\n'
 *     [charend..READBUF+count-1] does not contain a newline
 *   Otherwise on exit-
 *     FullLine == 0
 **/
{ register char *q;
  char save;

  q = READBUF + SrcBuffer->count;
  save = p[-1]; p[-1] = '\n';
  while (*(--q) != '\n') ;
  p[-1] = save;
  if (p <= q) { SrcBuffer->charend = q + 1; return 1; }

  q = READBUF + SrcBuffer->count - 1;	/* Last might be \r of \r\n */
  save = p[-1]; p[-1] = '\r';
  while (*(--q) != '\r') ;
  p[-1] = save;
  if (p <= q) { SrcBuffer->charend = q + 1; return 1; }

  return 0;
}

#if defined(__cplusplus) || defined(__STDC__)
void
refillBuf(char *p)
#else
void
refillBuf(p)
char *p;
#endif
/* Obtain additional information from the input file
 *   On entry-
 *     p addresses a character position within the current text buffer
 *   If there is no more information in the input file then on exit-
 *     The information pointed to by p is unchanged
 *     TEXTSTART points to the information previously pointed to by p
 *     The contents of the memory pointed to by p are undefined
 *   Otherwise on exit-
 *     The information pointed to by p is augmented by at least one line
 *     That entire line, including its terminating  '\n', is in memory
 *     TEXTSTART points to the information previously pointed to by p
 *     The contents of the memory pointed to by p are undefined
 ***/
{
#ifdef MONITOR
  _dapto_enter ("source");
#endif

  if (!SrcBuffer) {	/* make sure initBuf was called first!! */
    (void)fprintf(stderr,"refillBuf: module never initialized\n");
#ifdef MONITOR
    _dapto_leave ("source");
#endif
    exit(1);
  }
  /* Guaranteed by the buffer invariant:
   *   READBUF+count addresses the first free character position
   *   0 <= count
   *   charend <= READBUF+count
   *   sentinelSav == character saved from charend[0] (if significant)
   */

  if (p < SrcBuffer->memblock || p > SrcBuffer->charend ||
      (p != SrcBuffer->charend && *p == NUL)) {
    (void)fprintf(stderr,"refillBuf: invalid argument\n");
#ifdef MONITOR
    _dapto_leave ("source");
#endif
    exit(1);
  }
  /* memblock <= p <= charend
   * [p..charend-1] == text the client wishes to retain
   */

  *(SrcBuffer->charend) = SrcBuffer->sentinelSav;
  /* [charend..READBUF+count-1] == text not yet made available to the client
   */

  SrcBuffer->charend = p;
  /* memblock <= charend <= READBUF+count
   * [charend..READBUF+count-1] text remaining in the buffer
   */

  if (READBUF+SrcBuffer->count == SrcBuffer->charend) {	/* Empty buffer */
    TEXTSTART = READBUF; SrcBuffer->count = 0;
  } else if (READBUF > SrcBuffer->charend) {	/* Text in partial-line area */
    TEXTSTART = SrcBuffer->charend;
  } else {			/* Text should be moved down */
    /* READBUF <= charend < READBUF+count
     */
    char *temp = SrcBuffer->memblock;	/* Current buffer area */
    size_t MoveCount;			/* Number of characters to be moved */
    size_t DestIndex;			/* Move to READBUF[-DestIndex] */

    MoveCount = (READBUF + SrcBuffer->count) - SrcBuffer->charend;
    /* charend[0..MoveCount-1] == text remaining in the buffer
     * 0 < MoveCount
     */

    DestIndex = MoveCount % READSIZ;
    if (DestIndex == 0) DestIndex = READSIZ;
    /* 0 < DestIndex <= READSIZ
     */

    if (DestIndex > SrcBuffer->maxline) {	/* re-size the buffer */
      SrcBuffer->maxline = DestIndex;
      if (!(SrcBuffer->memblock =
             (char *)malloc(
               (size_t)(SrcBuffer->maxline+SrcBuffer->readsiz+1)))) {
        Exception("refillBuf (Text buffer)");
      }
    }
    /* 0 < DestIndex <= maxline <= READSIZ
     */

    TEXTSTART = READBUF - DestIndex;
    SrcBuffer->count = MoveCount - DestIndex;
    /* memblock <= TEXTSTART
     * count < MoveCount
     * TEXTSTART+MoveCount == READBUF+count
     */

    /* Cannot use memcpy to move the text within the buffer because the
     * source and destination areas may overlap.  Memcpy is unreliable
     * in that case.  The loop implements the following assignment:
     *
     *   TEXTSTART[0..MoveCount-1] = charend[0..MoveCount-1];
     *
     * for MoveCount>0
     */
    { register char *dest = TEXTSTART;
      register char *src = SrcBuffer->charend;
      register size_t size = MoveCount;

      do *dest++ = *src++; while (--size);
    }
    /* TEXTSTART[0..MoveCount-1] == text remaining in the buffer
     */

    if (temp != SrcBuffer->memblock) (void)free((void *)temp);
  }
  /* [TEXTSTART..READBUF+count-1] == text remaining in the buffer
   * TEXTSTART <= READBUF+count
   */

  /* Obtain more input information.  The additional input is concatenated
   * to the partial line by virtue of where it is read.  Guarantee at
   * least one newline in the new information.
   */
  { size_t first = SrcBuffer->count;
    /* Loop invariant:
     *   [TEXTSTART..READBUF+count-1] == complete text read so far
     *   0 <= first
     *   TEXTSTART <= READBUF+first <= READBUF+count
     *   READBUF[first..count-1] == new text read
     */
    while ((first == SrcBuffer->count || !FullLine(READBUF + first)) &&
           !SrcBuffer->EndOfFile) {
      size_t space = SrcBuffer->readsiz - SrcBuffer->count;
      size_t added;

      if (space == 0) {	/* Increase the buffer size */
        size_t text = TEXTSTART - SrcBuffer->memblock;

        SrcBuffer->readsiz += READSIZ;	/* Obtain space for another block */
        if (!(SrcBuffer->memblock =
               (char *)realloc(
                 (void *)SrcBuffer->memblock,
                 (size_t)(SrcBuffer->maxline+SrcBuffer->readsiz+1)))) {
          Exception("refillBuf (Text buffer)");
        }
        TEXTSTART = SrcBuffer->memblock + text;
        space = READSIZ;
      }
      /* space > 0 */

      first = SrcBuffer->count;
      added = read(SrcBuffer->fd, READBUF + first, space);

      if (added == (size_t) -1) { /* read signalled an error by returning -1 */
        Exception("refillBuf");
      }

      if (added > 0) SrcBuffer->count += added;
      else SrcBuffer->EndOfFile = 1;
    }
    /* [TEXTSTART..READBUF+count-1] == complete text read so far
     * TEXTSTART < charend <= READBUF+count && charend[-1] == '\n' ||
     *   TEXTSTART <= READBUF+count && EndOfFile
     */

    if (SrcBuffer->EndOfFile) {
      /* TEXTSTART <= READBUF+count
       */
      if (TEXTSTART < (READBUF + SrcBuffer->count) &&
          READBUF[SrcBuffer->count-1] != '\n')
        READBUF[SrcBuffer->count++] =  '\n';

      /* TEXTSTART == READBUF+count ||
       *   TEXTSTART < READBUF+count && READBUF[count-1] == '\n'
       */
      SrcBuffer->charend = READBUF + SrcBuffer->count;
    }
  }
  /* [TEXTSTART..charend-1] == text to be delivered to the client
   * [charend..READBUF+count-1] == partial line to be retained
   * TEXTSTART == charend == READBUF+count ||
   *   TEXTSTART < charend <= READBUF+count && charend[-1] == '\n'
   */

  SrcBuffer->sentinelSav = *SrcBuffer->charend; *SrcBuffer->charend = NUL;

  /* [TEXTSTART..charend-1] == text to be delivered to the client
   * charend[0] == NUL
   * [sentinelSav, charend+1..READBUF+count-1] == partial line to be retained
   * TEXTSTART == charend == READBUF+count ||
   *   TEXTSTART < charend <= READBUF+count && charend[-1] == '\n'
   */

#ifdef MONITOR
  _dapto_leave ("source");
#endif
}


#if defined(__cplusplus) || defined(__STDC__) 
void
initBuf(const char *name, int f)
#else
void
initBuf(name, f)
char *name; int f;
#endif
/* Create a source text buffer
 *   On entry-
 *     name is the symbolic name of the input file
 *     f is the descriptor for the input file
 *     The input file has been opened successfully
 *   On exit-
 *     SrcBuffer points to a new text buffer
 *     SRCFILE is the symbolic name of the input file
 *     If the input file is empty then TEXTSTART points to NUL
 *     Otherwise TEXTSTART points to the first character of the first line
 *       of the input file.  The entire line, including its terminating
 *       '\n', is in memory
 ***/
{
#ifdef MONITOR
  _dapto_enter ("source");
#endif

  if (!(SrcBuffer = (SrcBufPtr)malloc((size_t)(sizeof(SrcBuf)+strlen(name))))) {
  /* Note that the SrcBuf structure allocates one character position to the
   * field "name", so by adding strlen(name) to the size of a SrcBuf we
   * actually have strlen(name)+1 character positions available -- enough
   * for the string AND its terminating NUL.
   */
    perror("initBuf (SrcBuffer)");
#ifdef MONITOR
    _dapto_leave ("source");
#endif
    exit(1);
  }
  SrcBuffer->fd = f;		/* current input file descriptor */
  SrcBuffer->EndOfFile = 0;	/* no end-of-file read yet */
  if (!(SrcBuffer->memblock = (char *)malloc((size_t)(MAXLINE+READSIZ+1)))) {
  /* one extra in case the last character read is a newline (then we want
   * to zap the next location)
   */
    Exception("initBuf (memblock)");
  }
  SrcBuffer->maxline = MAXLINE;	/* Initial size of the partial line area */
  SrcBuffer->readsiz = READSIZ;	/* Initial size of the read area */
  SrcBuffer->charend = READBUF;	/* Location of first undelivered character */
  SrcBuffer->count=0;		/* Number of bytes in the read area */
  (void)strcpy((char *)SRCFILE, name);

  refillBuf(READBUF);

#ifdef MONITOR
  _dapto_leave ("source");
#endif
}


int
finlBuf()
/* Finalize the current source text buffer
 *   On entry-
 *     SrcBuffer points to the current source text buffer
 *   On exit-
 *     finlBuf is the descriptor for the buffer's input file
 *     All storage for the current source text buffer has been freed
 *     SrcBuffer=0
 ***/
{
  int fd;

#ifdef MONITOR
  _dapto_enter ("source");
#endif

  if (!SrcBuffer) {     /* make sure initBuf was called first!! */
    (void)fprintf(stderr,"finlBuf: module never initialized\n");
    exit(1);
  }

  (void)free((void *)SrcBuffer->memblock);
  fd = SrcBuffer->fd;
  (void)free((void *)SrcBuffer);
  SrcBuffer = (SrcBufPtr)0;

#ifdef MONITOR
  _dapto_leave ("source");
#endif

  return fd;
}
