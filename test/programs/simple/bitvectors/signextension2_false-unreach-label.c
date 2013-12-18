#include <stdio.h>

int main() {
  unsigned int allOne = -1;
  
  int castToInt = allOne;
  long castToLong = allOne;
  long castToLong2 = castToInt;
  unsigned long castToULong = allOne;
  unsigned long castToULong2 = castToInt;

  if (castToInt == -1 && castToLong == 4294967295L &&
      castToLong2 == -1 && castToULong == 4294967295L &&
      castToULong2 == 18446744073709551615ul) {
    printf ("UNSAFE\n");
    goto ERROR;
  }

  printf ("SAFE\n");

  return (0);
  ERROR:
  return (-1);
}

