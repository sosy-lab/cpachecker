extern int __VERIFIER_nondet_int();
extern int input();

int __SELECTED_FEATURE_PLUS;

int validProduct() {
	if (__SELECTED_FEATURE_PLUS) {
		return 1;
	}
	return 0;
}

int main() {
	int x = input();
	
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		if (x < 10) {
			G1: x = 10;	
		}
	}

	return 0;
}
