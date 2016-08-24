# 1 "for2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "for2/main.c"
int main()
{
  int i;
  int k;

  for (i=0; i<3; i++)
  {
    k=3;
    continue;
    k=4;
  }

  assert(0);
}
