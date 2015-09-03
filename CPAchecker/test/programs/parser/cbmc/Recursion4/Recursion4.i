# 1 "Recursion4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Recursion4/main.c"
int factorial2(int i)
{
  if(i==0) return 1;


  int tmp_result;
  tmp_result=factorial2(0);
  return tmp_result;
}

int main()
{
  int result_factorial;
  result_factorial=factorial2(1);
  assert(result_factorial==1);
  return 0;
}
