#include <assert.h>

int mThread=0;
int start_main=0;
int mStartLock=0;
int __COUNT__ =0;

void main() { //nsThread::Main (mozilla/xpcom/threads/nsThread.cpp 1.31)

  int self = mThread;
  while (start_main==0);
  while(mStartLock != 0);
  //  START_NOENV;
  mStartLock = 2;
  //  END_NOENV;
  mStartLock = 0;
  if( __COUNT__ == 1 ) { // atomic check(1);
    //START_NOENV;

    int rv = self; // self->RegisterThreadSelf();
    __COUNT__ = __COUNT__ + 1;
    //    END_NOENV;

  } else { assert(0); } 
}


