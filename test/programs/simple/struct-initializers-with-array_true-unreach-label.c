struct s {
  int x;
  char c[1];
};
void main() {
  struct s s = { 0, 0 };
  if (s.x != 0) {
ERROR:
    return;
  }
}
