int mThread=0;
int start_main=0;
int mStartLock=0;
int __COUNT__ =0;

void main() { //nsThread::Main (mozilla/xpcom/threads/nsThread.cpp 1.31)

  int self = mThread;
  while (start_main==0);
  acquire(mStartLock);
  release(mStartLock);
  { __blockattribute__((atomic))
      if( __COUNT__ == 1 ) { // atomic check(1);
	int rv = self; // self->RegisterThreadSelf();
	__COUNT__ = __COUNT__ + 1;
      } else { assert(0); } 
  }
}


