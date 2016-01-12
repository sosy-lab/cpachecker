extern int __VERIFIER_nondet_int();

int __SELECTED_FEATURE_PLUS;

int validProduct() {
	if (__SELECTED_FEATURE_PLUS) {
		return 1;
	}
	return 0;
}

int main() {
	int x = 0;
	
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		G1: x = 10;	
	}

	return 0;
}
