# 1 "Pointer_Arithmetic7/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic7/main.c"
void f(char *p)
{
  p[1]=1;
}

int main ()
{
  char dummy[10];
  f(dummy);
}
