int f()
{
  return 2;
}

int main()
{
  int a;
  a = f();
  if(a!=2){
    f();
    ERROR: return -1;
  }
  return 0;
}
