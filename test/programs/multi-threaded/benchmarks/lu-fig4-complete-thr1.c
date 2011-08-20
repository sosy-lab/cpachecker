#include "lu-fig4.h"
#include <assert.h>

bool __START_ASYNC__ = False; // models if the second thread can start
int __COUNT__ = 0; // models a counter which monitors order violations


inline OSErr PBReadSync(int) { return noErr; }

// ====================== 2nd thread

static void asyncIOCompletion (ExtendedParamBlock pbAsyncPtr_thread)
{ // (mozilla/nsprpub/pr/src/md/mac/macthr.c 3.13)
  while (__START_ASYNC__ == False) {}; // second thread waits until the async read is issued
    PRThread thread = pbAsyncPtr_thread;

    if (_PR_MD_GET_INTSOFF()) {
		return;
    }
    _PR_MD_SET_INTSOFF(1);

	DoneWaitingOnThisThread(thread);

    _PR_MD_SET_INTSOFF(0);

}

inline void DoneWaitingOnThisThread(PRThread thread)
{
    int is;
    int thread_md_asyncIOLock;
    int thread_io_pending;
    int thread_md_asyncIOCVar;

	_PR_INTSOFF(is);
	PR_Lock(thread_md_asyncIOLock);
	/* atomic */
	if (__COUNT__ == 1) {
	  thread_io_pending = PR_FALSE; // check for order violation
	  __COUNT__ = __COUNT__ + 1;
	} else {
	  assert(0);
	}

	// let the waiting thread know that async IO completed 
	PR_NotifyCondVar(thread_md_asyncIOCVar);
	PR_Unlock(thread_md_asyncIOLock);
	_PR_FAST_INTSON(is);
}

inline bool _PR_MD_GET_INTSOFF() { return PR_FALSE; }
