# 1 "gcc_statement_expression1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "gcc_statement_expression1/main.c"
int main()
{
  int x;
  int y;


  ({ x=1; x;});

  assert(x==1);

  x= ({ y=1; 2; });

  assert(x==2);
  assert(y==1);




  int a=({ int b=(long int)&a; b; });

  return 0;
}
