int main() {
  char *a;
  char b[] = "abc";
  a = malloc(3);
  int i = __VERIFIER_nondet_int();
  if (i <= 4) {
    memcpy(a, b, i);
  }
  free(a);
  return 0;
}
