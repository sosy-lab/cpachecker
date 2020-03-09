struct s {
  int x;
  char c[1];
};

extern struct s nondet_struct_s();

void main() {
  struct s s1 = {1};
  struct s s2 = {2};
  s1 = nondet_struct_s();
  if (s2.x != 2) {
ERROR:
    return;
  }
}
