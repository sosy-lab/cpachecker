/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 71 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 3 "mutex-unsafe-thr0.c"
int cs1  =    0;
#line 4 "mutex-unsafe-thr0.c"
int cs2  =    0;
#line 5 "mutex-unsafe-thr0.c"
int lock  =    0;
#line 7 "mutex-unsafe-thr0.c"
void main(void) 
{ 

  {
  {
#line 8
  while (1) {
    while_0_continue: /* CIL Label */ ;
    {
#line 9
    while (1) {
      while_1_continue: /* CIL Label */ ;
#line 9
      if (lock != 0) {

      } else {
        goto while_1_break;
      }
    }
    while_1_break: /* CIL Label */ ;
    }
#line 10
    lock = 1;
#line 11
    cs1 = 1;
#line 12
    if (cs2 == 0) {

    } else {
      {
#line 12
      __assert_fail("cs2 == 0", "mutex-unsafe-thr0.c", 12U, "main");
      }
    }
#line 13
    cs1 = 0;
#line 14
    lock = 0;
  }
  while_0_break: /* CIL Label */ ;
  }
}
}