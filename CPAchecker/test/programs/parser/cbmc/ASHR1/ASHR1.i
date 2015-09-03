# 1 "ASHR1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "ASHR1/main.c"
int main()
{
  int x=-4, y;

  y=x>>1;
  x>>=1;
  assert(x==-2);
  assert(y==-2);


  assert(((-2)>>1u)==-1);


  x=-1;
  x=x>>1;
  assert(x==-1);

  x=-10;
  x=x>>10;
  assert(x==-1);
}
