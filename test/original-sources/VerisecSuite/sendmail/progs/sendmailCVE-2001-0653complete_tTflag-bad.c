
/*

MIT Copyright Notice

Copyright 2003 M.I.T.

Permission is hereby granted, without written agreement or royalty fee, to use, 
copy, modify, and distribute this software and its documentation for any 
purpose, provided that the above copyright notice and the following three 
paragraphs appear in all copies of this software.

IN NO EVENT SHALL M.I.T. BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, 
INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE 
AND ITS DOCUMENTATION, EVEN IF M.I.T. HAS BEEN ADVISED OF THE POSSIBILITY OF 
SUCH DAMANGE.

M.I.T. SPECIFICALLY DISCLAIMS ANY WARRANTIES INCLUDING, BUT NOT LIMITED TO 
THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, 
AND NON-INFRINGEMENT.

THE SOFTWARE IS PROVIDED ON AN "AS-IS" BASIS AND M.I.T. HAS NO OBLIGATION TO 
PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

$Author: tleek $
$Date: 2004/01/05 17:27:46 $
$Header: /mnt/leo2/cvs/sabo/hist-040105/sendmail/s6/tTflag-bad.c,v 1.1.1.1 2004/01/05 17:27:46 tleek Exp $



*/


/*

Sendmail Copyright Notice


Copyright (c) 1998-2003 Sendmail, Inc. and its suppliers.
     All rights reserved.
Copyright (c) 1983, 1995-1997 Eric P. Allman.  All rights reserved.
Copyright (c) 1988, 1993
     The Regents of the University of California.  All rights reserved.

By using this file, you agree to the terms and conditions set
forth in the LICENSE file which can be found at the top level of
the sendmail distribution.


$Author: tleek $
$Date: 2004/01/05 17:27:46 $
$Header: /mnt/leo2/cvs/sabo/hist-040105/sendmail/s6/tTflag-bad.c,v 1.1.1.1 2004/01/05 17:27:46 tleek Exp $



*/


/*

<source>

*/

/**
**  TtSETUP -- set up for trace package.
**
**	Parameters:
**		vect -- pointer to trace vector.
**		size -- number of flags in trace vector.
**		defflags -- flags to set if no value given.
**
**	Returns:
**		none
**
**	Side Effects:
**		environment is set up.
*/

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <getopt.h>
#include "sendmail.h"
#include <assert.h>

static u_char	*tTvect;
static int	tTsize;
static char	*DefFlags;

#define OPTIONS	"B:b:C:cd:e:F:f:Gh:IiL:M:mN:nO:o:p:q:R:r:sTtUV:vX:"

void
main()
{
	u_char *vect;
	int size;
	char *defflags;

        tTvect = vect;  /* vect is an alias for tTdvect, tTvect is an alias for vect */
	tTsize = size;
	DefFlags = defflags;
	
	char *s = malloc(size);
	tTflag(s);
}

/*
**  TtFLAG -- process an external trace flag description.
**
**	Parameters:
**		s -- the trace flag.
**
**	Returns:
**		none.
**
**	Side Effects:
**		sets/clears trace flags.
*/

void
tTflag(s)
	register char *s;
{
	int first, last;
	register unsigned int i;

	printf ("s: %s", s);

	if (*s == '\0')
		s = DefFlags;

	for (;;)
	{
		/* find first flag to set */
		i = 0;
               
		while (isascii(*s) && isdigit(*s))
			i = i * 10 + (*s++ - '0');
		
		/* assigning unsigned int to signed int */
		/* if i is a large positive number, first will become a negative number */
 		first = i;

             	/* find last flag to set */
		if (*s == '-')
		{
			i = 0;
			while (isascii(*++s) && isdigit(*s))
				i = i * 10 + (*s - '0');
		}
		last = i;
            
		/* find the level to set it to */
		i = 1;
		if (*s == '.')
		{
			i = 0;
			while (isascii(*++s) && isdigit(*s))
				i = i * 10 + (*s - '0');
		}

		/* clean up args */

		if (first >= tTsize) /* check will fail if first is negative!  */
			first = tTsize - 1;
		if (last >= tTsize)  
		  last = tTsize - 1;

		/* set the flags */
		while (first <= last){ /* this check will hold true for a while if */
		{                      /* first is negative and last is positive */
		
		/*von mir eingefügt*/
		assert(first >= 0);
		  printf("index = %d\n", first); 
		  /*BAD*/
		  tTvect[first++] = i; /* UNDERFLOW CAN OCCUR HERE. */
		                       /* tTvect can only hold tTsize elements */
		}
		  /* This is a potential BSS underflow of tTdvect defined inside sendmail.h */	
		}
		/* more arguments? */
		if (*s++ == '\0')
			return;
	}
}





/*

</source>

*/

