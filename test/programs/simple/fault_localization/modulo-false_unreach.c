extern int __VERIFIER_nondet_int();

int generateNumberDiv3(int seed){
	int old;
	do{
		old = seed;
		seed++;
	} while (old%3 != 0);
	return seed;
}

int main(){
	int div3 = generateNumberDiv3(__VERIFIER_nondet_int());
	if(div3 % 3 != 0) 
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
