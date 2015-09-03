# 1 "Function6/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function6/main.c"
int global;

void f()
{
  void g();

  g();
}

void g()
{
  global=123;
}

int main()
{
  f();
  assert(global==123);
  return 0;
}
