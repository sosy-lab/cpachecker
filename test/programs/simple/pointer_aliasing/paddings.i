void *kzalloc(unsigned long size);

void __VERIFIER_error(void) { ERROR: goto ERROR; }

struct s {
  char c[3];
  int g;
  int f;
};

int main() {
  struct s *ps = kzalloc(sizeof (struct s));
  struct s *ps2 = kzalloc(sizeof (struct s));
  ps2->f = 1;
  ps2->f = 1;
  if (ps != 0 && ps2 != 0 && ps->f != 0) __VERIFIER_error();
  return 0;
}
