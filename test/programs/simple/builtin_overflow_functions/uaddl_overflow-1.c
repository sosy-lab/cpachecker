#include <limits.h>
int main() {
  unsigned long int a = UINT_MAX/2;
  signed long int b = UINT_MAX/2;
  unsigned long int res = 0;
  int error = __builtin_uaddl_overflow(a,b,&res);
  // test whether res is correctly calculated in case there is no overflow
  if (res != 4294967294) {
    goto ERROR;
  }
  // test whether the return value is correctly calculated to false if there is an overflow
  if (error) {
    ERROR:
    return 1;
  }
  return 0;
}
