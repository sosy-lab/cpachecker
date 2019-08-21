#include <stdio.h>
int a()
{
  printf("a\n");
}

int b()
{
  printf("b\n");
}  


int main()
{
  int (*p[2])();
  
  p[0] = a;
  p[1] = b;
  p[0]();
  p[1]();
}


