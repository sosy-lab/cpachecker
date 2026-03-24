extern void reach_error();

void f(int x) {
  if (x > 1000000) {
    return;
  }
  if (x % 2) {
    reach_error();
  }
  x += 2;
  f(x);
}

int main() {
  f(0);
  return 0;
}
