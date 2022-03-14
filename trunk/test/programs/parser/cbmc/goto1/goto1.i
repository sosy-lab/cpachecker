# 1 "goto1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "goto1/main.c"
int main()
{
  int i, j;

  if(i)
    goto l;

  if(j)
    goto l;

  assert(!i && !j);

 l:;
}
