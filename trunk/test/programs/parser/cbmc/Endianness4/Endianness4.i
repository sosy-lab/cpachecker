# 1 "Endianness4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Endianness4/main.c"
void main()
{
  int x;
  char * cp = &x;

  for (int i=0; i!= sizeof(int); i++)
    *(cp+i) = 0;


  assert(x==0);
}
