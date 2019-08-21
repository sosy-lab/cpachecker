# 1 "Endianness3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Endianness3/main.c"
int main()
{
  unsigned int x;
  unsigned char *p;

  x=0xffff;

  p=(unsigned char *)&x;

  *p=1;


  assert(x==0xff01);
}
