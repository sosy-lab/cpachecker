extern int __VERIFIER_nondet_int();
extern int input();

int __SELECTED_FEATURE_LE;
int __SELECTED_FEATURE_GR;
int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;
int __SELECTED_FEATURE_ABS;

int validProduct() {
	if (((__SELECTED_FEATURE_LE && !__SELECTED_FEATURE_GR) || (!__SELECTED_FEATURE_LE && __SELECTED_FEATURE_GR))
			&& ((__SELECTED_FEATURE_PLUS && !__SELECTED_FEATURE_MINUS) || (!__SELECTED_FEATURE_PLUS && __SELECTED_FEATURE_MINUS))
			&& (!__SELECTED_FEATURE_ABS || __SELECTED_FEATURE_MINUS)) {
		return 1;
	}
	return 0;
}

int foobar(int x, int y, int z) {
	int a;
	int x = input();
	int y = input();
	int z = input();

	__SELECTED_FEATURE_GR = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_LE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_ABS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();
	
	if (__SELECTED_FEATURE_LE) {
		if (x < y)
			G1: a = x;
		else
			G2: a = y;
	} else if (__SELECTED_FEATURE_GR) {
		if (x > y)
			G3: a = x;
		else
			G4: a = y;
	}
	if (__SELECTED_FEATURE_PLUS) {
		G5: z += a;
	} else if (__SELECTED_FEATURE_MINUS) {
		if (__SELECTED_FEATURE_ABS) {
			if ((z - a) < 0)
				G6: a *= -1;
		}
		G7: z -= a;
	}
	G8: return z;
}


int main() {
	int x = input();
	int y = input();
	int z = input();

	__SELECTED_FEATURE_GR = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_LE = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_ABS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();

	if (validProduct()) {
		foobar(x, y, z);
	}

	return 0;
}
