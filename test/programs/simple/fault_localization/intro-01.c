extern int __VERIFIER_nondet_int();

int main(){

	//calculate 10%3 manually
	int a = 10;
	int b = 3;

	while(a >= 0) 
		a -= b;
	a = -a - b;

	//POST-CONDITION check if manual computation is right (10%3 = 1)
	if(a!=1)
		goto ERROR;
	

EXIT: return 0;
ERROR: return 1;
}
