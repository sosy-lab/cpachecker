/*
** 2001 September 15
**
** The author disclaims copyright to this source code.  In place of
** a legal notice, here is a blessing:
**
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
**
*************************************************************************
** This file contains code to implement the "sqlite" command line
** utility for accessing SQLite databases.
**
** $Id: shell.c,v 1.189 2008/12/04 12:26:01 drh Exp $
*/
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>
#include "sqlite3.h"
#include <ctype.h>
#include <stdarg.h>

#if !defined(_WIN32) && !defined(WIN32) && !defined(__OS2__)
# include <signal.h>
# if !defined(__RTP__) && !defined(_WRS_KERNEL)
#  include <pwd.h>
# endif
# include <unistd.h>
# include <sys/types.h>
#endif


# define readline(p) local_getline(p,stdin)
# define add_history(X)
# define read_history(X)
# define write_history(X)
# define stifle_history(X)


char *local_getline(char *zPrompt, FILE *in){
  char *zLine;
  int nLine;
  int n;
  int eol;

  if( zPrompt && *zPrompt ){
    printf("%s",zPrompt);
    fflush(stdout);
  }
  nLine = 100;
  zLine = malloc( nLine );
  n = 0;
  eol = 0;
  while( !eol ){
    if( n+100>nLine ){
      nLine = nLine*2 + 100;
      zLine = realloc(zLine, nLine);
      if( zLine==0 ) return 0;
    }
    if( fgets(&zLine[n], nLine - n, in)==0 ){
      if( n==0 ){
        free(zLine);
        return 0;
      }
      zLine[n] = 0;
      eol = 1;
      break;
    }
    while( zLine[n] ){ n++; }
    if( n>0 && zLine[n-1]=='\n' ){
      n--;
      zLine[n] = 0;
      eol = 1;
    }
  }
  zLine = realloc( zLine, n+1 );
  return zLine;
}


int do_meta_command(){
  int i = 1;
  int nArg = 0;
  FILE *in = stdin;
  int n, c;
  int rc = 0;
  char *zLine;
  int nSep = 40;
  int lineno = 0;
  char *azArg[50];
  int nCol = 100;

  /* Parse the input line into tokens.
  */
    while( (zLine = local_getline(0, in))!=0 ){
      char *z;
      i = 0;
      lineno++;
      for(i=0, z=zLine; *z && *z!='\n' && *z!='\r'; z++){
        
          *z = 0;
          i++;
          if( i<nCol ){
            z += nSep-1;
          }
        
      }
      *z = 0;
      if( i+1!=nCol ){
        fprintf(stderr,"line %d: expected %d columns of data but found %d\n", lineno, nCol, i+1);
        break;
      }
      free(zLine);
    }
    fclose(in);
    return 0;
}

