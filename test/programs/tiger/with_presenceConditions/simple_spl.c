extern int __VERIFIER_nondet_int();

int __SELECTED_FEATURE_LE;
int __SELECTED_FEATURE_GR;
int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;

int validProduct() {
	if (((__SELECTED_FEATURE_LE && !__SELECTED_FEATURE_GR)
			|| (!__SELECTED_FEATURE_LE && __SELECTED_FEATURE_GR))
			&& ((__SELECTED_FEATURE_PLUS && !__SELECTED_FEATURE_MINUS)
					|| (!__SELECTED_FEATURE_PLUS && __SELECTED_FEATURE_MINUS))) {
		return 1;
	}
	return 0;
}

int foobar(int x, int y, int z) {
	int a;

	if (__SELECTED_FEATURE_LE) {
		G1: a = x;
	} else if (__SELECTED_FEATURE_GR) {
		G2: a = y;
	}
	if (__SELECTED_FEATURE_PLUS) {
		G3: z += a;
	} else if (__SELECTED_FEATURE_MINUS) {
		G4: z -= a;
	}

	a = 1;
	if (a == 10) {
		G5: z = z;
	}

	return z;
}

int main() {
	int x = input();
	int y = input();
	int z = input();

	__SELECTED_FEATURE_GR = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_LE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		foobar(x, y, z);
	}

	return 0;
}
