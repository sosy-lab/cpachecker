int main(void) {

  char a[] = "abc";
//  char b[] = { "abc" };
  char *c = { "abc" };
  const char * d[2] = {"a","b"};

  if (a[0] != 'a') return 0;
  if (a[1] != 'b') return 0;
  if (a[2] != 'c') return 0;
  if (a[3] != 0) return 0;

  /*
  if (b[0] != 'a') return 0;
  if (b[1] != 'b') return 0;
  if (b[2] != 'c') return 0;
  if (b[3] != 0) return 0;
  */

  if (c[0] != 'a') return 0;
  if (c[1] != 'b') return 0;
  if (c[2] != 'c') return 0;
  if (c[3] != 0) return 0;

  if (d[0][0] != 'a') return 0;
  if (d[0][1] != 0) return 0;
  if (d[1][0] != 'b') return 0;
  if (d[1][1] != 0) return 0;

ERROR:
  return 1;
}
