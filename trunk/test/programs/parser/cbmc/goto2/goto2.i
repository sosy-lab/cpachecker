# 1 "goto2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "goto2/main.c"
int main()
{
  int i, j;

  i=1;

  if(j)
    goto l;

  i=2;

 l:;

  assert(i==1 || !j);
}
