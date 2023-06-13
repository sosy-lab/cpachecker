# 1 "String5/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "String5/main.c"
unsigned my_strlen(const char *s)
{

  unsigned x=0;
  while(*s) { s++; x++; }
  return x;
}

int main()
{
 int l;

 l=my_strlen("abcXYZ");

 assert(l==6);
}
