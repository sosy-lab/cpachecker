typedef unsigned int u32;

typedef union U {
  long pad;
  u32 a;
} U;

int main(void) {
  unsigned int x = 123u;
  U u = (U) x;

  if (u.a != (u32)x) goto error;
  return 0;

error:
  return -1;
}
