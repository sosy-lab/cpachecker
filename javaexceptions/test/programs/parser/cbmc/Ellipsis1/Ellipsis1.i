# 1 "Ellipsis1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Ellipsis1/main.c"
void f(int a, ...) {
  assert(a==1);
}

int main() {
  f(1, 2, 3);
}
