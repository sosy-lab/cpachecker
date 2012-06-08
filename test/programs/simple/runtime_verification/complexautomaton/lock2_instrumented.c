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
int __MONITOR_STATE_lockStatus   = -1;

void __initialize__(void) ;

#line 1 "lock2.c"
extern void init() ;

#line 2
extern void lock() ;

#line 3
extern void unlock() ;

#line 5 "lock2.c"
int entry(void) 
{ int initialized ;
  int lastLock ;
  int n ;
  int i ;

  {
  {
#line 6
  initialized = 0;
  {
#line 19 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 20
  if (__MONITOR_STATE_lockStatus != -1) {
#line 21
    error_fn();
  } else {
#line 23
    __MONITOR_STATE_lockStatus = 0;
  }
#line 25
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 7 "lock2.c"
  init();
#line 8
  initialized = 1;
  {
#line 35 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 36
  if (__MONITOR_STATE_lockStatus == 1) {
#line 37
    error_fn();
  } else {
#line 39
    __MONITOR_STATE_lockStatus = 1;
  }
#line 41
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 10 "lock2.c"
  lock();
  {
#line 51 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 52
  if (__MONITOR_STATE_lockStatus == 0) {
#line 53
    error_fn();
  } else {
#line 55
    __MONITOR_STATE_lockStatus = 0;
  }
#line 57
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 11 "lock2.c"
  unlock();
  {
#line 35 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 36
  if (__MONITOR_STATE_lockStatus == 1) {
#line 37
    error_fn();
  } else {
#line 39
    __MONITOR_STATE_lockStatus = 1;
  }
#line 41
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 13 "lock2.c"
  lock();
  {
#line 51 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 52
  if (__MONITOR_STATE_lockStatus == 0) {
#line 53
    error_fn();
  } else {
#line 55
    __MONITOR_STATE_lockStatus = 0;
  }
#line 57
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 14 "lock2.c"
  unlock();
#line 16
  lastLock = 0;
  {
#line 35 "spec.work"
  __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 36
  if (__MONITOR_STATE_lockStatus == 1) {
#line 37
    error_fn();
  } else {
#line 39
    __MONITOR_STATE_lockStatus = 1;
  }
#line 41
  __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
  {

  }
  }
#line 18 "lock2.c"
  lock();
#line 19
  i = 1;
  }
#line 19
  while (i < n) {
#line 20
    if (! initialized) {
      {

      {
#line 19 "spec.work"
      __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 20
      if (__MONITOR_STATE_lockStatus != -1) {
#line 21
        error_fn();
      } else {
#line 23
        __MONITOR_STATE_lockStatus = 0;
      }
#line 25
      __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
      {

      }
      }
#line 21 "lock2.c"
      init();
      }
    }
#line 22
    if (i - lastLock == 2) {
      {
#line 23
      lastLock += 2;
      {
#line 35 "spec.work"
      __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 36
      if (__MONITOR_STATE_lockStatus == 1) {
#line 37
        error_fn();
      } else {
#line 39
        __MONITOR_STATE_lockStatus = 1;
      }
#line 41
      __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
      {

      }
      }
#line 24 "lock2.c"
      lock();
      }
    } else {
      {

      {
#line 51 "spec.work"
      __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
#line 52
      if (__MONITOR_STATE_lockStatus == 0) {
#line 53
        error_fn();
      } else {
#line 55
        __MONITOR_STATE_lockStatus = 0;
      }
#line 57
      __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
      {

      }
      }
#line 27 "lock2.c"
      unlock();
      }
    }
    {
#line 19
    i ++;
    }
  }
#line 31
  return (1);
}
}

void __initialize__(void) 
{ 

  {

}
}

