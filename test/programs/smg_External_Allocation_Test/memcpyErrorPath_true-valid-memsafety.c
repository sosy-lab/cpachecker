int main() {
  char *a;
  char b[] = "ab";
  a = malloc(3);
  int i = __VERIFIER_nondet_int();
  if (i <= 3) {
    memcpy(a, b, i);
  }
  free(a);
  return 0;
}
