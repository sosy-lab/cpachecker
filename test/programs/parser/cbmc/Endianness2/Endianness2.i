# 1 "Endianness2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Endianness2/main.c"
int main()
{
  unsigned int u=1;
  unsigned char *p;
  unsigned char x, y;

  p=(unsigned char *)&u;

  x=*p;


  assert(x==0);

  y=p[3];

  assert(y==1);
}
