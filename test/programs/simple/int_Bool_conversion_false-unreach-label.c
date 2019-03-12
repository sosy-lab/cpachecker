struct s {
  _Bool b : 1;
};

int main (void) {
  struct s s;
  s.b = 2;
  _Bool b = 2;
  if (s.b != 1 || b != 1) {
    return 0;
  }
ERROR:
  return 1;
}
