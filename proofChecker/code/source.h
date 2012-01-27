#ifndef SOURCE_H
#define SOURCE_H

/* $Id: source.h,v 2.4 1997/09/04 18:19:53 waite Exp $ */
/* Copyright 1993, The Regents of the University of Colorado */

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

#include <stdlib.h>

typedef struct {	/* Definition of a source text buffer */
  int fd;		  /* File descriptor */
  int EndOfFile;	  /* Nonzero when the end of the file has been seen */
  char sentinelSav;	  /* Character replaced by a sentinel */
  size_t maxline;	  /* reads occur at this offset */
  size_t readsiz;	  /* Number of bytes in the read buffer */
  char *memblock;	  /* Storage area */
  char *charend;	  /* delimits boundary between useful lines
			     and a last partial line */
  char *TextStart;	  /* First interesting location */
  size_t count;		  /* number of bytes read */
  char name[1];		  /* Symbolic name of the file */
} SrcBuf, *SrcBufPtr;

extern SrcBufPtr SrcBuffer;	/* Current source text buffer */

#define TEXTSTART (SrcBuffer->TextStart)
#define SRCFILE (SrcBuffer->name)

#if defined(__cplusplus) || defined(__STDC__)

extern void initBuf(const char *infile, int f);
/* Create a source text buffer
 *   On entry-
 *     infile is the symbolic name of the input file
 *     f is the descriptor for the input file
 *     The input file has been opened successfully
 *   On exit-
 *     SrcBuffer points to a new text buffer
 *     SRCFILE is the symbolic name of the input file
 *     If the input file is empty then TEXTSTART points to '\0'
 *     Otherwise TEXTSTART points to the first character of the first line
 *       of the input file.  The entire line, including its terminating
 *       '\n', is in memory
 ***/

extern void refillBuf(char *p);
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

extern int finlBuf();
/* Finalize the current source text buffer
 *   On entry-
 *     SrcBuffer points to the current source text buffer
 *   On exit-
 *     finlBuf is the descriptor for the buffer's input file
 *     All storage for the current source text buffer has been freed
 *     SrcBuffer=0
 ***/

#else
extern void refillBuf();
extern void initBuf();
extern int finlBuf();
#endif

#endif
