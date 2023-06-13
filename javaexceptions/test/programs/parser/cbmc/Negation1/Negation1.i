# 1 "Negation1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Negation1/main.c"
int test;

int main()
{
  test=0;
  test=~test;
  assert(test==-1);

  test=0;
  test=!test;
  assert(test==1);

  test=100;
  test=!test;
  assert(test==0);

  assert(!!100==1);
}
