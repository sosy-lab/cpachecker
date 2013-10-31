#include <stdio.h>

int main() {
  //unsigned int plus_one = 1;
  //int minus_one = -1;
  char t[4];
  t[0] = 0;
  t[1] = 1;
  t[2] = 0;
  t[3] = 0;
  char* z1 = t;
  void* z2 = z1;
  unsigned int* pi = z2;
  if (*pi != 256) {
    printf ("UNSAFE\n");
    goto ERROR;
  }
  printf ("SAFE\n");

  //if(plus_one < minus_one) {
  //  goto ERROR;
  //}
  
  return (0);
  ERROR:
  return (-1);
}

