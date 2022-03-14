# 1 "Fixedbv5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Fixedbv5/main.c"
int main()
{
  float a, b;

  __CPROVER_assume(a==1 || a==0.5 || a==2 || a==3 || a==0.1);
  b=a;
  a/=2;
  a*=2;
  __CPROVER_assert(a==b, "float div works");
}
