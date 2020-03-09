extern int __VERIFIER_nondet_int();
struct s {
  int x;
  char c[1];
};
void main() {
  struct s s1 = {1,0};
  struct s s2;
  if (s1.x != 1 && s2.x != 0) {
ERROR:
    return;
  }
}
