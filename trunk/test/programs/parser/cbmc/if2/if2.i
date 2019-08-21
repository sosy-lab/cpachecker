# 1 "if2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "if2/main.c"
int nondet_int();

int main()
{
  int i, j, k;

  i=nondet_int();
  k=nondet_int();

  if(i)
  {
  }
  else
  {
    if(k)
    {
      assert(0);
    }
  }
}
