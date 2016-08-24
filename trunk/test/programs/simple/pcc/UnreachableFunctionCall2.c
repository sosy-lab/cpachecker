int f()
{
  return 2;
}

int main()
{ int x=0;
  int a;
  a = f();
  if(a!=2){
    f();
    ERROR: x=-1;
  }
  return x;
}
