# 1 "goto4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "goto4/main.c"
int g=0;

int f()
{
  assert(g==0);
  g++;
}

int main()
{
l:
l2:;
  int i=f();
  goto l;
}
