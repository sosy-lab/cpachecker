# 1 "Recursion2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Recursion2/main.c"
void f(unsigned int counter) {
  if(counter==0) return;

  f(counter-1);
}

int main() {
  unsigned int x;
  __CPROVER_assume(x<=10);

  f(x);

}
