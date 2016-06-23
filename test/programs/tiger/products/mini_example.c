extern int __VERIFIER_nondet_int();
extern int input();

int a;

int validProduct() {
	if (a) {
		return 1;
	}
	return 0;
}

int main() {
	int x = input();
	a = __VERIFIER_nondet_int();
	
	if (validProduct()) {
		if (x <= 10) {
			G1: x = 1;
		}
	}
		
	return 0;
}
