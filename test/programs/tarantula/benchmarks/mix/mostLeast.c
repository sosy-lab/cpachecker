extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

void __VERIFIER_assert(int cond)
{
  if (!(cond))
  {
    ERROR: __VERIFIER_error();
  }
  return;
}

void foo(int in1,int in2,int in3){
	int least;
	int most;
	least = in1;
	most = in1;
	if (most < in2){
		most = in2;
	}
	if (most < in3){
		most = in3;
	}
	if (least > in2){ 
		most = in2; // bug: most = in2 instead of least = in2
	}
	if (least > in3){ 
		least = in3; 
	}
	__VERIFIER_assert(least <= most);
}
int main()
{
foo(__VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());

  return 0;
}