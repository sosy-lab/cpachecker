# 1 "Initialization2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Initialization2/main.c"
int nondet_int();

int Test = nondet_int();

int f()
{
  return 1;
}


int g=f();

int main()
{
  assert(g==1);
}
