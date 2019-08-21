# 1 "if1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "if1/main.c"
int main()
{
  int i, j;

  i = 1;

  if(j > 0)
    j += i;
  else
    j = 0;

  assert(i != j);
}
