#include <stdio.h>

int main() {
  unsigned char t[4];
  t[0] = -1;
  t[1] = -1;
  t[2] = -1;
  t[3] = -1;
  unsigned char* z1 = t;
  void* z2 = z1;
  unsigned int* pi = z2;
  
  if (*pi != -1) {
    printf ("UNSAFE\n");
    goto ERROR;
  }
  printf ("SAFE\n");
  
  return (0);
  ERROR:
  return (-1);
}

