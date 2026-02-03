//#include<stdio.h>

typedef union  {
  short a;
  short b;
  int x; //target of cast not first member of union
  short c;
} uv;

int main() {
  uv value = {.x = 100000};

  if (value.x != 100000) {
    goto error;
  }
  return 0;
error:
  return -1;
}
