/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 71 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 3 "mutex-safe-thr0.c"
int cs1  =    0;
#line 4 "mutex-safe-thr0.c"
int cs2  =    0;
#line 5 "mutex-safe-thr0.c"
int lock  =    0;
#line 8 "mutex-safe-thr0.c"
void main(void) 
{ 

  {
  {
#line 9
  while (1) {
    while_0_continue: /* CIL Label */ ;
    {
#line 10
    while (1) {
      while_1_continue: /* CIL Label */ ;
#line 10
      if (lock != 0) {

      } else {
        goto while_1_break;
      }
    }
    while_1_break: /* CIL Label */ ;
    }
#line 11
    _START_NOENV_;
    lock = 1;
    _END_NOENV_;
#line 12
    cs1 = 1;
#line 13
    if (cs2 == 0) {

    } else {
      {
#line 13
      __assert_fail("cs2 == 0", "mutex-safe-thr0.c", 13U, "main");
      }
    }
#line 14
    cs1 = 0;
#line 15
    lock = 0;
  }
  while_0_break: /* CIL Label */ ;
  }
}
}