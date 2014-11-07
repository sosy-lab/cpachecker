//ABE  invariant not feasible to implement
int f(int var1, int var2)
{
 int y = var1+var2;
 y = y+2;
 return y;
}

int main()
{
  int a=0;
  int x;
  
  if(x<0){
    x=0;
    a = f(x, a);
  }
  else
  {
    a = f(x,a);
  }
  a++;
  if(a<3){
    ERROR: return -1;
  }

  return 0;
}
