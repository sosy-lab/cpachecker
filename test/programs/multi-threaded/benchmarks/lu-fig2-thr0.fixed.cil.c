/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 71 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 3 "lu-fig2-thr0.fixed.c"
int mThread  =    0;
#line 4 "lu-fig2-thr0.fixed.c"
int start_main  =    0;
#line 5 "lu-fig2-thr0.fixed.c"
int mStartLock  =    0;
#line 6 "lu-fig2-thr0.fixed.c"
int __COUNT__  =    0;
#line 9 "lu-fig2-thr0.fixed.c"
void main(void) 
{ int PR_CreateThread__RES ;

  {
#line 11
  PR_CreateThread__RES = 1;
  {
#line 12
  while (1) {
    while_0_continue: /* CIL Label */ ;
#line 12
    if (mStartLock != 0) {

    } else {
      goto while_0_break;
    }
  }
  while_0_break: /* CIL Label */ ;
  }
#line 14
  mStartLock = 1;
#line 16
  start_main = 1;
#line 18
  if (__COUNT__ == 0) {
#line 20
    mThread = PR_CreateThread__RES;
#line 21
    __COUNT__ = __COUNT__ + 1;
  } else {
    {
#line 23
    __assert_fail("0", "lu-fig2-thr0.fixed.c", 23U, "main");
    }
  }
#line 25
  mStartLock = 0;
#line 26
  if (mThread == 0) {
#line 26
    return;
  } else {
#line 27
    return;
  }
}
}