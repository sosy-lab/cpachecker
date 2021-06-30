/* This software was developed at the National Institute of Standards and Technology by employees of the Federal Government
in the course of their official duties. Pursuant to title 17 Section 105 of the United States Code
this software is not subject to copyright protection and is in the public domain.
NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.

We would appreciate acknowledgement if the software is used.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAXSIZE    40

/* Replacement to memset() that cannot be optimized out */
char *my_memset_s(char *s, int c, size_t n)
{
	volatile char *p = s;

	if(p != NULL)
		while (n--)
			*p++ = c;

	return s;
}

void
test( char * str )
{
  char * buf;
  char * oldbufaddress;

  buf = malloc( MAXSIZE );
  if ( !buf )
    return;

  strncpy( buf, str, MAXSIZE );

  buf[MAXSIZE - 1] = '\0';

  printf( "original buffer content is : %s\n", buf );

  oldbufaddress = buf;

  buf = malloc( 1024 );
  if( !buf )
  {
    my_memset_s( oldbufaddress, 0, MAXSIZE );
	free( oldbufaddress );
	return;
  }
  strcpy( buf, oldbufaddress );
  my_memset_s( oldbufaddress, 0, MAXSIZE );		/* FIX */

  printf( "realloced buffer content is : %s\n", buf );
  printf( "original buffer address content is : %s\n", oldbufaddress );

  free( oldbufaddress );
  free( buf );
}



int
main( int argc, char * * argv )
{
  char * userstr;

  if ( argc > 1 )
  {
    userstr = argv[1];
    test( userstr );
  }

  return 0;
}

