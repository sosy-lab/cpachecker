int mThread=0;
int start_main=0;
int __COUNT__ =0;


int thr1() { //nsThread::Init (mozilla/xpcom/threads/nsThread.cpp 1.30)
  
  int PR_CreateThread__RES = 1;
  start_main=1;

      if( __COUNT__ == 0 ) { // atomic check(0);
	mThread = PR_CreateThread__RES; 
	__COUNT__ = __COUNT__ + 1; 
      } else { assert(0); } 

  if (mThread == 0) { return -1; }
  else { return 0; }

}


void thr2() { //nsThread::Main (mozilla/xpcom/threads/nsThread.cpp 1.30)

  int self = mThread;
  while (start_main==0);

      if( __COUNT__ == 1 ) { // atomic check(1);
	int rv = self; // self->RegisterThreadSelf();
	__COUNT__ = __COUNT__ + 1;
      } else { assert(0); } 


}


