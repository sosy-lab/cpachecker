/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 71 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 3 "simple-thr1-safe.c"
int g  =    0;
#line 4 "simple-thr1-safe.c"
int cs1  =    0;
#line 5 "simple-thr1-safe.c"
int cs2  =    0;
#line 7 "simple-thr1-safe.c"
void main(void) 
{ 

  {
#line 8
  cs1 = 1;
#line 10
  cs1 = 0;
#line 11
  g = 1;
#line 12
  return;
}
}