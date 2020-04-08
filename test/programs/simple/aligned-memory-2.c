extern void* malloc(unsigned long);
extern void __VERIFIER_error();

int main() {
  char a[2];
  char b;

  if (((long)&b) % 4 == 0) {
    goto EXIT;
  }

  if (((long)&a) % 4 == 0) {
    goto EXIT;
  }

ERROR:
  return 1;
EXIT:
  return 0;
}
