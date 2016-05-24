extern int __VERIFIER_nondet_int(void);

int flag = 0;
int main()
{
  int x = 20;
  int y = __VERIFIER_nondet_int();
  if(x<=0 || y<=0)
  {
	  return -1;
  }
  int quo = 0;
  flag=quo;
  int y0 = y;
  while(y<= x)
  {
	  quo=quo+1;
	  flag=quo;
	  y=y+y0;
	  if(y<0)
	  {
		  return -1;
	  }
  }
  return quo;
} 
