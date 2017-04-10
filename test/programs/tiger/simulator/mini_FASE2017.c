extern int __VERIFIER_nondet_int();
extern int input();

int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;
int __SELECTED_FEATURE_LOL;

int validProduct() {
	if (__SELECTED_FEATURE_PLUS || (!__SELECTED_FEATURE_PLUS && __SELECTED_FEATURE_MINUS)) {
		return 1;
	}
	return 0;
}

int main() {
	int x = input();
	
	

	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_LOL = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		if (x < 10 && __SELECTED_FEATURE_PLUS) {
			G1: x = 10;	
		}
		if (x < 10 && __SELECTED_FEATURE_MINUS) {
			G2: x = 10;	
		}

	}

	return 0;
}
