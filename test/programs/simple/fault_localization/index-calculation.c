extern int __VERIFIER_nondet_int();

int main(){
	int x = __VERIFIER_nondet_int();
	if (x != 1) 
		x = 2;
	else 
		x = x + 2;
	
	if(!(x < 3 && x >= 0))
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
}
