extern int __VERIFIER_nondet_int();

/** Find a number greater than seed that is divisble by 3 */
int generateNumberDiv3(int seed){
	int old;
	do{
		old = seed;
		seed++;
	} while (old%3 != 0);
	return seed;
}

int main(){
	// Generate a number that is divisible by 3
	int seed = __VERIFIER_nondet_int();
	int div3 = generateNumberDiv3(seed);

	// POST-CONDITION check if the number is divisble by 3
	if(div3 % 3 != 0) 
		goto ERROR;
EXIT: return 0;
ERROR: return 1;
}
