# 1 "Function_Pointer1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Function_Pointer1/main.c"
void foo()
{


  void *p=foo;
}

int x = 0;

void f1(void)
{
  x = 1;
}

void call(void (*f)())
{
  f();
}

int main()
{
  call(f1);
}
