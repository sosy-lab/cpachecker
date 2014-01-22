
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
$Date: 2004/01/05 17:27:45 $
$Header: /mnt/leo2/cvs/sabo/hist-040105/sendmail/s6/my-main.c,v 1.1.1.1 2004/01/05 17:27:45 tleek Exp $



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
$Date: 2004/01/05 17:27:45 $
$Header: /mnt/leo2/cvs/sabo/hist-040105/sendmail/s6/my-main.c,v 1.1.1.1 2004/01/05 17:27:45 tleek Exp $



*/


/*

<source>

*/

#include <stdio.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <getopt.h>
#include <sendmail.h>

#define OPTIONS	"B:b:C:cd:e:F:f:Gh:IiL:M:mN:nO:o:p:q:R:r:sTtUV:vX:"

#define LEN 100
unsigned char tTdvect[LEN];


int
main(argc, argv, envp)
	int argc;
	char **argv;
	char **envp;
{
	int j;

	tTsetup(tTdvect, LEN, "0-99.1");
	
	while ((j = getopt(argc, argv, OPTIONS)) != -1)
	  {
	    switch (j)
	      {
	      case 'd':
		/* hack attack -- see if should use ANSI mode */
		if (strcmp(optarg, "ANSI") == 0)
		  {
		    break;   
		  }
		tTflag(optarg);
		setbuf(stdout, (char *) NULL);
		break;
		
	      case 'G':	/* relay (gateway) submission */  
		break;
	  
	      case 'L':
		break;
		
	      case 'U':	/* initial (user) submission */
		break;
	      }
	  }
	
	
	return 0;


}

/*

</source>

*/

