extern int __VERIFIER_nondet_int();

int a;

int validProduct() {
	if (a) {
		return 1;
	}
	return 0;
}

int main() {
	int x = __VERIFIER_nondet_int();
	a = __VERIFIER_nondet_int();
	
	if (validProduct()) {
		G1: x = 1;
	}
		
	return 0;
}
