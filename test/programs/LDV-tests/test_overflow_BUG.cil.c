/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 180 "/usr/include/bits/types.h"
typedef long __ssize_t;
#line 110 "/usr/include/sys/types.h"
typedef __ssize_t ssize_t;
#line 341 "/usr/include/stdio.h"
extern int printf(char const   * __restrict  __format  , ...) ;
#line 69 "/usr/include/assert.h"
extern  __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const   *__assertion ,
                                                                      char const   *__file ,
                                                                      unsigned int __line ,
                                                                      char const   *__function ) ;
#line 5 "test_overflow_BUG.c"
int VERDICT_UNSAFE  ;
#line 7 "test_overflow_BUG.c"
int CURRENTLY_UNSAFE  ;
#line 9
ssize_t getService(void) ;
#line 10 "test_overflow_BUG.c"
int globalSize  ;
#line 12 "test_overflow_BUG.c"
int main(int argc , char **argv ) 
{ int retVal ;
  ssize_t tmp ;

  {
  {
#line 15
  tmp = getService();
#line 15
  retVal = (int )tmp;
  }
#line 16
  if (sizeof(retVal) == (unsigned long )globalSize) {

  } else {
    {
#line 16
    __assert_fail("sizeof(retVal)==globalSize", "test_overflow_BUG.c", 16U, "main");
    }
  }
  {
#line 17
  printf((char const   * __restrict  )"returned value: %d\n", retVal);
  }
#line 18
  return (0);
}
}
#line 22 "test_overflow_BUG.c"
ssize_t getService(void) 
{ ssize_t localVar ;

  {
  {
#line 23
  localVar = 999999999999L;
#line 24
  globalSize = (int )sizeof(localVar);
#line 25
  printf((char const   * __restrict  )"localVar: %d\n", localVar);
  }
#line 26
  return (localVar);
}
}