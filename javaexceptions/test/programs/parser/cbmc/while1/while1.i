# 1 "while1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "while1/main.c"
void cmp_str(char* s1, char* s2, int n) {

  while ((n > 0) && (*s1 == *s2)) {
    s1++;
    s2++;
    n--;
  }

  assert ((n <= 0) || (*s1 != *s2));
}

char nondet_char();



int main () {
  char a[3];
  char b[3];
  a[0] = 2;
  b[0] = 2;

  for (int i = 1; i < 3; i++) {
    a[i] = nondet_char();
    b[i] = nondet_char();
  }

  cmp_str(a, b, 3);

  return 0;
}
