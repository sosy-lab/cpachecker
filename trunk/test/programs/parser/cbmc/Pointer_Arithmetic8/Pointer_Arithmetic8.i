# 1 "Pointer_Arithmetic8/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic8/main.c"
void f(int *);

int main()
{
  int a[5];

  f(a);
}

void f(int *p)
{
  p[10]=0;
}
