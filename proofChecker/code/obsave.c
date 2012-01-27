static char RCSid[] = "$Id: obsave.c,v 1.1 1998/06/04 15:23:24 mjung Exp $";
/* Copyright 1998, The Regents of the University of Colorado */

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

#include "obsave.h"

typedef struct {                /* Base of obstack restore data */
  void *mark;                     /* First free location */
  struct chunkelt *chunks;        /* List of chunks */
  Obstack control;                /* obstack definition */
} SaveElement;

typedef struct chunkelt {       /* Chunk restore data */
  struct chunkelt *chunks;        /* Remainder of the chunks */
  void *restoreTo;                /* Where to restore the data */
  void *thisChunk;                /* Data for this chunk */
  size_t howBig;                  /* Amount of data for this chunk */
} ChunkElement;

static Obstack save_obstk;
static ObstackP Saver = (ObstackP)0;

void *
#ifdef PROTO_OK
SaveObstack(ObstackP data, void *mark)
#else
SaveObstack(data, mark) ObstackP data; void *mark;
#endif
/* Save the state of an obstack
 *   On entry-
 *     data points to the obstack whose state is to be saved
 *     Else mark=0
 *   On exit-
 *     SaveObstack points to the saved state
 ***/
{ SaveElement *base;
  ChunkElement temp;
  struct _obstack_chunk *c;

  if (!Saver) {Saver = &save_obstk; obstack_init(Saver); }

  /* if no mark is given, the obstack should only contain one
   * currently growing object */
    
  temp.chunks = (ChunkElement *)0;
  for (c = data->chunk; c; c = c->prev) {
    temp.restoreTo = c;
    temp.howBig = c->limit - (char *)c;
    temp.thisChunk = obstack_copy(Saver, c, temp.howBig);
    temp.chunks = (ChunkElement *)obstack_copy(Saver, &temp, sizeof(ChunkElement));
  }

  base = (SaveElement *)obstack_alloc(Saver, sizeof(SaveElement));
  base->mark = mark;
  base->chunks = temp.chunks;
  memcpy(&(base->control), data, sizeof(Obstack));

  return (void *)base;
}

#define BASE ((SaveElement *)base)

void
#ifdef PROTO_OK
RestoreObstack(ObstackP data, void *base)
#else
RestoreObstack(data, base) ObstackP data; void *base;
#endif
/* Restore the state of an obstack
 *   On entry-
 *     data points to the obstack whose state is to be saved
 *     base points to the saved state
 ***/
{ if (BASE->mark) {
    ChunkElement *temp;

    obstack_free(data, BASE->mark);
    memcpy(data, &(BASE->control), sizeof(Obstack));
    for (temp = BASE->chunks; temp; temp = temp->chunks)
      memcpy(temp->restoreTo, temp->thisChunk, temp->howBig);
  } else {
    /* Verify that in the obstack, the growing object has not been
     * fixed in the meantime */

    data->next_free =
      (char *)data->chunk + (BASE->control.next_free - (char *)BASE->control.chunk);
    memcpy((char *)data->chunk, BASE->chunks->thisChunk, BASE->chunks->howBig);
  }
}

#undef BASE

typedef struct dataelt {          /* Static Data restore data */
  void *restoreTo;                /* Where to restore the data */
  void *data;                     /* Data */
  size_t howBig;                  /* Amount of data for this chunk */
} DataDescriptor;


void *
#ifdef PROTO_OK
SaveData(void *data, size_t length)
#else
SaveData(data, length) void *data; size_t length;
#endif
/* Save the state of static data
 *   On entry-
 *     data points to the data whose state is to be saved
 *     length contains the length of the data.
 *   On exit-
 *     Savedata points to the saved state
 ***/
{
  DataDescriptor *desc;

  if (!Saver) {Saver = &save_obstk; obstack_init(Saver); }

  desc = (DataDescriptor *)obstack_alloc(Saver, sizeof(DataDescriptor));
  desc->restoreTo = data;
  desc->howBig = length;
  desc->data = (void *)obstack_copy(Saver, data, desc->howBig);

  return (void *)desc;
}

#define BASE ((DataDescriptor *)base)

void
#ifdef PROTO_OK
RestoreData(void *base)
#else
RestoreData(base) void *base;
#endif
/* Restore the state of static data
 *   On entry-
 *     base points to the saved state
 ***/
{
   memcpy(BASE->restoreTo, BASE->data, BASE->howBig);
}

#undef BASE
