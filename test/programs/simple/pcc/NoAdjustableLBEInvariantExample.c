// depending on when it is noticed that path not feasible ( cpa.predicate.satCheck>0<threshold), adjustable LBE  invariant not feasible to implement

int main()
{
  int a=0;
  int x=5;
  
  while(x>0){
    if(x%2==0){
      a++;
    }
    a++;
    x--;
  }
  if(a!=7){
    ERROR: return -1;
  }

  return 0;
}