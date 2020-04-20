
int main() {
  int a[5];
  int *p = a;
  if (a+1 == p+1) {
ERROR:
    return 1;
  }
  return 0;
}
