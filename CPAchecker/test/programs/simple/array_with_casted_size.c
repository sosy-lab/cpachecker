typedef unsigned long myint;

struct s {
  int a[(myint)10];
} s = { 0, 1, 2, 3, 4 };

int main() {
  if (s.a[5] != 0) {
ERROR:
    return 1;
  }
  return 0;
}
