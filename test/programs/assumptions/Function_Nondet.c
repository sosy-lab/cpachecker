extern int __VERIFIER_nondet_int();

int main() {


	int number = __VERIFIER_nondet_int();
	int check;

	if(number < 0) {
		check = 4;
	} else if(number == 0) {
		check = 5;
	} else {
		check = __VERIFIER_nondet_int();
	}
	
	if((check - 5) < number) {
	ERROR: return -1;
	}

	// no error
	return 0;
}