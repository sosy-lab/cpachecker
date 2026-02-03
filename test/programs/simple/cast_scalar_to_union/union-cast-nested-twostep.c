typedef union Inner {
  float f;
  int i;
} Inner;

typedef union Outer {
  double d;
  Inner in;
} Outer;

int main(void) {
  int x = 7;

  Inner a = (Inner) x;   // must choose i
  Outer o = (Outer) a;  // must choose in

  if (o.in.i != 7) goto error;
  return 0;

error:
  return -1;
}
