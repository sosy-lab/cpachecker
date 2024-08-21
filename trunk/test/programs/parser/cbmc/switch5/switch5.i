# 1 "switch5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "switch5/main.c"
int main()
{
  unsigned int i, j;

  switch(i)
  {
  case 10:
    j=10;
    break;

  default:;
    j=i+1;
  }


  assert(j==i+1);
}
