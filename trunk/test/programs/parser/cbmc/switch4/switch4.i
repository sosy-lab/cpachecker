# 1 "switch4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "switch4/main.c"
main()
{
  int x;

  switch(x)
  {
  case 0:
    goto end;

  default:
    x = 0;
  }

 end:
  assert(x==0);
}
