# 1 "struct3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "struct3/main.c"
int main() {
  struct
  {
    int a, b;
  } s, q;

  s=q;

  assert(s.a==q.a);
}
