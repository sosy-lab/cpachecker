# 1 "for-break1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "for-break1/main.c"
int main()
{
  int a=0, b=0;
  int x;

  for(x=0; x<3; x++)
  {
    break;

    b=1;
  }

  assert(a==b);
}
