
struct foo { int i; char c; }

bar (struct foo *a) {
  if (a->c == 0) {
    *((char *)a + sizeof(int)) = 1;
    if (a->c != 0)
      abort();
  }
}

