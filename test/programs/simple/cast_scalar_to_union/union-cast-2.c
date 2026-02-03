//#include<stdio.h>

typedef union  {
  struct {
    unsigned char a;
    unsigned char b;
    unsigned char c;
    unsigned char d;
  } st;
  int x; //target of cast not first member of union
} uv;

int main() {
  uv value = (uv) 8191;
//  printf("a: %d b: %d c: %d d: %d\n", value.st.a, value.st.b, value.st.c, value.st.d);

  if (value.st.a != 255) {
    goto error;
  }
  if (value.st.b != 31) {
    goto error;
  }
  if (value.st.c != 0 || value.st.d != 0) {
    goto error;
  }
  return 0;
error:
  return -1;
}
