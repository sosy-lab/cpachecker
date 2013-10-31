# 1 "void_ifthenelse/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "void_ifthenelse/main.c"
int g;

void f()
{
  g=1;
}

int main() {
  assert(g==0);

  g==0?f():g;

  assert(g==1);
}
