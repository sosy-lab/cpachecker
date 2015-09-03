# 1 "Pointer_Arithmetic6/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic6/main.c"
int main()
{
  int a[10];
  int x;

  a[1]=1000;

  x=*(a+1);

  assert(x==1000);
}
