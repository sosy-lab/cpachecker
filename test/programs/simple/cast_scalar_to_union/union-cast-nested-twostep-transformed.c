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

  Inner a = (Inner){ .i = x };
  Outer o = (Outer){ .in = a };

  if (o.in.i != 7) goto error;
  return 0;

error:
  return -1;
}
