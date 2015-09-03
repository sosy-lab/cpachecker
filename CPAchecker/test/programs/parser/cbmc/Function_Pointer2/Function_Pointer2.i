# 1 "Function_Pointer2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer2/main.c"
int global;

void f(int farg)
{
  global=1;
}

void g(int garg)
{
  global=0;
}

int main()
{
  void (*p)(int);
  int c = 1;

  p=c?f:g;

  p(1);

  assert(global==c);
}
