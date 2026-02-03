typedef union U {
  int i;
  void *p;
} U;

int main(void) {
  void *q = (void*)0x1234;
  U u = (U) q;      // must initialize member p

  if (u.p != q) goto error;
  return 0;

error:
  return -1;
}
