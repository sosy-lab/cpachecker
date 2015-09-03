# 1 "return1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "return1/main.c"

short ret_const()
{
  return 123;
}


unsigned long save_res0, save_res1;


int f0()
{
  short res = 0;

  res += ret_const();



  save_res0 = res;

  return 0;
}


int f1()
{
  short res = 0;

  res += ret_const();



  save_res1 = res;

  return 0;
}


int main()
{
  f0();
  f1();

  assert( save_res0 == save_res1 );
}
