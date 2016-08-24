# 1 "Pointer_Arithmetic5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic5/main.c"
int x, y;

void f()
{
  int *px=&x;

  x=1;
  px++;


  y=*px;
}
