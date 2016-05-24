int fib(int n)
{
  int  i, Fnew, Fold, temp,ans;

    Fnew = 1;  Fold = 0;
    i = 2;
    while( i <= n ) {
      temp = Fnew;
      Fnew = Fnew + Fold;
      Fold = temp;
      i++;
    }
    ans = Fnew;
  return ans;
}
 
  int flag = 5; 
    
int main()
{
  flag = fib(flag);
  return flag;   
}



