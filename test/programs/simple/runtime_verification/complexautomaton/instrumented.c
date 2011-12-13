#line 1 "include.h"
void error_fn(void) 
{ 

  {
  ERROR: 
  goto ERROR;
}
}

#line 5 "include.h"
int __MONITOR_START_TRANSITION   = 0;

#line 6 "include.h"
int __MONITOR_END_TRANSITION   = 0;

#line 2 "spec.work"
int __BLAST_error  ;

#line 3 "spec.work"
void __error__(void) 
{ 

  {
#line 5
  __BLAST_error = 0;
  ERROR: 
  goto ERROR;
}
}

#line 8 "spec.work"
void __BLAST___error__(void) 
{ 

  {
#line 10
  __BLAST_error = 0;
  BERROR: 
  goto BERROR;
}
}

#line 13 "spec.work"
int __MONITOR_STATE_lockStatus   = 0;

void __initialize__(void) ;

#line 1 "locktest.c"
extern void init() ;

#line 2
extern void lock() ;

#line 3
extern void unlock() ;

#line 5 "locktest.c"
int entry(void) 
{ int lastLock ;
  int i ;

  {
  {

  {
#line 18 "spec.work"
  __MONITOR_STATE_lockStatus = 0;
  {

  }
  }
#line 6 "locktest.c"
  init();
  {
#line 27 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 28
  if (__MONITOR_STATE_lockStatus == 1) {
#line 29
    error_fn();
  } else {
#line 31
    __MONITOR_STATE_lockStatus = 1;
  }
#line 33
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 8 "locktest.c"
  lock();
  {
#line 43 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 44
  if (__MONITOR_STATE_lockStatus == 0) {
#line 45
    error_fn();
  } else {
#line 47
    __MONITOR_STATE_lockStatus = 0;
  }
#line 49
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 9 "locktest.c"
  unlock();
  {
#line 27 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 28
  if (__MONITOR_STATE_lockStatus == 1) {
#line 29
    error_fn();
  } else {
#line 31
    __MONITOR_STATE_lockStatus = 1;
  }
#line 33
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 11 "locktest.c"
  lock();
  {
#line 43 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 44
  if (__MONITOR_STATE_lockStatus == 0) {
#line 45
    error_fn();
  } else {
#line 47
    __MONITOR_STATE_lockStatus = 0;
  }
#line 49
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 12 "locktest.c"
  unlock();
#line 14
  lastLock = 0;
  {
#line 27 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 28
  if (__MONITOR_STATE_lockStatus == 1) {
#line 29
    error_fn();
  } else {
#line 31
    __MONITOR_STATE_lockStatus = 1;
  }
#line 33
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 16 "locktest.c"
  lock();
#line 17
  i = 1;
  }
#line 17
  while (i < 1000) {
#line 18
    if (i - lastLock == 2) {
#line 18
      if (i < 999) {
        {
#line 19
        lastLock += 2;
        {
#line 27 "spec.work"
        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 28
        if (__MONITOR_STATE_lockStatus == 1) {
#line 29
          error_fn();
        } else {
#line 31
          __MONITOR_STATE_lockStatus = 1;
        }
#line 33
        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
        {

        }
        }
#line 20 "locktest.c"
        lock();
        }
      } else {
        {

        {
#line 43 "spec.work"
        __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 44
        if (__MONITOR_STATE_lockStatus == 0) {
#line 45
          error_fn();
        } else {
#line 47
          __MONITOR_STATE_lockStatus = 0;
        }
#line 49
        __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
        {

        }
        }
#line 23 "locktest.c"
        unlock();
        }
      }
    } else {
      {

      {
#line 43 "spec.work"
      __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 44
      if (__MONITOR_STATE_lockStatus == 0) {
#line 45
        error_fn();
      } else {
#line 47
        __MONITOR_STATE_lockStatus = 0;
      }
#line 49
      __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
      {

      }
      }
#line 23 "locktest.c"
      unlock();
      }
    }
    {
#line 17
    i ++;
    }
  }
#line 27
  return (1);
}
}

void __initialize__(void) 
{ 

  {

}
}

