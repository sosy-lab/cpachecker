# 1 "return_void/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "return_void/main.c"
void f()
{
}

void g()
{
  return f();
}

int main()
{
  g();
}
