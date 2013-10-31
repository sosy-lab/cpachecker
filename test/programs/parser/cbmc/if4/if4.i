# 1 "if4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "if4/main.c"
int main()
{
  int x;

  __CPROVER_assume(x==1);

  if(x==2)
    x++;


  assert(x==1);
}
