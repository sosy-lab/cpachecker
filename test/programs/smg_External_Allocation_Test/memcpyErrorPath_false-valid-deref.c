int main() {
  char *a;
  char b[] = "ab";
  a = malloc(4);
  int i = __VERIFIER_nondet_int();
  if (i <= 4) {
    memcpy(a, b, i);
  }
  free(a);
  return 0;
}
