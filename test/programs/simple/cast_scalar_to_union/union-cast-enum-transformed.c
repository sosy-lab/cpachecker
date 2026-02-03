enum E { A = 1, B = 2 };

typedef union U {
  int i;
  enum E e;
} U;

int main(void) {
  enum E x = B;
  U u = (U){ .e = x };

  if (u.e != B) goto error;
  return 0;

error:
  return -1;
}
