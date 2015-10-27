extern int __VERIFIER_nondet_int();

int __SELECTED_FEATURE_FOOBAR_SPL = 1;
int __SELECTED_FEATURE_LE;
int __SELECTED_FEATURE_GR;
int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;
int __SELECTED_FEATURE_NOTNEGATIVE;

int validProduct() {
	if (__SELECTED_FEATURE_FOOBAR_SPL
		&& ((__SELECTED_FEATURE_LE && !__SELECTED_FEATURE_GR) || (!__SELECTED_FEATURE_LE && __SELECTED_FEATURE_GR))
		&& ((__SELECTED_FEATURE_PLUS && !__SELECTED_FEATURE_MINUS) || (!__SELECTED_FEATURE_PLUS && __SELECTED_FEATURE_MINUS))
		&& (!__SELECTED_FEATURE_NOTNEGATIVE || __SELECTED_FEATURE_MINUS)) {
		return 1;
	}
	return 0;
}

int foobar(int x, int y, int z) {
	int a;
	
	if (__SELECTED_FEATURE_LE) {
		if (x < y)
			G7: a = x;
		else
			G6: a = y;
	} else if (__SELECTED_FEATURE_GR) {
		if (x > y)
			G5: a = x;
		else
			G4: a = y;
	}
	if (__SELECTED_FEATURE_PLUS) {
		G1: z += a;
	} else if (__SELECTED_FEATURE_MINUS) {
		if (__SELECTED_FEATURE_NOTNEGATIVE) {
			if ((z - a) < 0)
				G3: a *= -1;
		}
		G2: z -= a;
	}
	return z;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int z = __VERIFIER_nondet_int();

	__SELECTED_FEATURE_GR = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_LE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_NOTNEGATIVE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		foobar(x, y, z);
	}

	return 0;
}
