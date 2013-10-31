# 1 "Endianness1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Endianness1/main.c"
int main()
{
  unsigned int u=1;
  unsigned char *p;
  unsigned char x, y;

  p=(unsigned char *)&u;

  x=*p;


  assert(x==1);

  y=p[1];

  assert(y==0);
}
