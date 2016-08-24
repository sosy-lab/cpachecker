//#include <stdio.h>
#include "hello.h"

struct header_struct Aa = {.a=5};
static int sameName = 4;


void hello (const char * name) {
  struct header_struct s;
  s.a = 1;
 // printf ("Hello, %s!\n", name);
}
