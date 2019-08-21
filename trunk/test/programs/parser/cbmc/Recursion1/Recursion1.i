# 1 "Recursion1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Recursion1/main.c"
void f(int counter) {
  if(counter==0) return;

  f(counter-1);
}

int main() {

  f(10);

}
