#include <assert.h>

int mThread=0;
int start_main=0;
int mStartLock=0;
int __COUNT__ =0;


void main() { //nsThread::Init (mozilla/xpcom/threads/nsThread.cpp 1.31)

  int PR_CreateThread__RES = 1;
  while(mStartLock != 0);
  //START_NOENV;
  mStartLock = 1;
  //END_NOENV;
  start_main=1;

  if( __COUNT__ == 0 ) { // atomic check(0);
    //START_NOENV;
    mThread = PR_CreateThread__RES; 
    __COUNT__ = __COUNT__ + 1; 
    //    ENV_NOENV;
  } else { assert(0); } 

  mStartLock = 0;
  if (mThread == 0) { return -1; }
  else { return 0; }

}



