# 1 "Static2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Static2/main.c"
int f()
{
  static int s=0;
  s++;
  return s;
}

int g()
{
  int l=0;
  l++;
  return l;
}

int main()
{
  assert(f()==1);
  assert(f()==2);
  assert(g()==1);
  assert(g()==1);
}
