extern int __VERIFIER_nondet_int(void);

void main() {
  int x;
  int y=1;
  int flag;
  int f = 0;
  int dump = __VERIFIER_nondet_int();
  
  if (dump)
  {
	  flag = 1;
  }
  else
  {
	  flag = 0;
  }
  
  if (dump)
  {
	  f = 1;
  }
  
  if (flag)
  {
	  y=f;
  } 
}
