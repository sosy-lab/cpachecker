/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
int t1  =    0;
int t2  =    0;
int x  ;

/* Thread 0 */
void thread0(void)
{
  {
    {
      while (1) {
      while_0_continue: /* CIL Label */ ;
        t1 = t2 + 1;
        {
          while (1) {
          while_1_continue: /* CIL Label */ ;
            if (t1 >= t2) {
              if (t2 > 0) {

              } else {
                goto while_1_break;
              }
            } else {
              goto while_1_break;
            }
          }
        while_1_break: /* CIL Label */ ;
        }
        x = 0;
        if (x <= 0) {

        } else {
          {
            __assert_fail("x <= 0", "bakery.simple-thr0.c", 11U, "main");
          }
        }
        t1 = 0;
      }
    while_0_break: /* CIL Label */ ;
    }
  }
}

/* Thread 1 */
void thread1(void)
{
  {
    {
      while (1) {
      while_0_continue: /* CIL Label */ ;
        t2 = t1 + 1;
        {
          while (1) {
          while_1_continue: /* CIL Label */ ;
            if (t2 >= t1) {
              if (t1 > 0) {

              } else {
                goto while_1_break;
              }
            } else {
              goto while_1_break;
            }
          }
        while_1_break: /* CIL Label */ ;
        }
        x = 1;
        if (x >= 1) {

        } else {
          {
            __assert_fail("x >= 1", "bakery.simple-thr1.c", 11U, "main");
          }
        }
        t2 = 0;
      }
    while_0_break: /* CIL Label */ ;
    }
  }
}