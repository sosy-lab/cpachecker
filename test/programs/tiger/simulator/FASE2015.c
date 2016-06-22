extern int __VERIFIER_nondet_int();
extern int __VERIFIER_nondet_bool();

int __SELECTED_FEATURE_FOOBAR_SPL;
int __SELECTED_FEATURE_LE;
int __SELECTED_FEATURE_GR;
int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;
int __SELECTED_FEATURE_NOTNEGATIVE;
int __SELECTED_FEATURE_COMP;
int __SELECTED_FEATURE_OP;

int validProduct() {
	if (__SELECTED_FEATURE_FOOBAR_SPL  &&  (!__SELECTED_FEATURE_FOOBAR_SPL  ||  __SELECTED_FEATURE_COMP)  &&  (!__SELECTED_FEATURE_FOOBAR_SPL  ||  __SELECTED_FEATURE_OP)  &&  (!__SELECTED_FEATURE_COMP  ||  __SELECTED_FEATURE_FOOBAR_SPL)  &&  (!__SELECTED_FEATURE_OP  ||  __SELECTED_FEATURE_FOOBAR_SPL)  &&  (!__SELECTED_FEATURE_NOTNEGATIVE  ||  __SELECTED_FEATURE_FOOBAR_SPL)  &&  (!__SELECTED_FEATURE_COMP  ||  __SELECTED_FEATURE_LE  ||  __SELECTED_FEATURE_GR)  &&  (!__SELECTED_FEATURE_LE  ||  __SELECTED_FEATURE_COMP)  &&  (!__SELECTED_FEATURE_GR  ||  __SELECTED_FEATURE_COMP)  &&  (!__SELECTED_FEATURE_LE  ||  !__SELECTED_FEATURE_GR)  &&  (!__SELECTED_FEATURE_OP  ||  __SELECTED_FEATURE_PLUS  ||  __SELECTED_FEATURE_MINUS)  &&  (!__SELECTED_FEATURE_PLUS  ||  __SELECTED_FEATURE_OP)  &&  (!__SELECTED_FEATURE_MINUS  ||  __SELECTED_FEATURE_OP)  &&  (!__SELECTED_FEATURE_PLUS  ||  !__SELECTED_FEATURE_MINUS)  &&  (!__SELECTED_FEATURE_NOTNEGATIVE  ||  __SELECTED_FEATURE_MINUS)  &&  (__SELECTED_FEATURE_LE  ||  __SELECTED_FEATURE_PLUS  ||  __SELECTED_FEATURE_NOTNEGATIVE  ||  __SELECTED_FEATURE_GR  ||  __SELECTED_FEATURE_MINUS  ||  1)) {
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

int avc(int a) {
	return 0;
}

int main() {
	int o1;
	int i1 = __VERIFIER_nondet_int();
	int i2 = __VERIFIER_nondet_int();
	int i3 = __VERIFIER_nondet_int();

	__SELECTED_FEATURE_FOOBAR_SPL = __VERIFIER_nondet_bool();
	__SELECTED_FEATURE_GR = __VERIFIER_nondet_bool();
	__SELECTED_FEATURE_LE = __VERIFIER_nondet_bool();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_bool();
	__SELECTED_FEATURE_NOTNEGATIVE = __VERIFIER_nondet_bool();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_bool();
    __SELECTED_FEATURE_COMP = __VERIFIER_nondet_bool();
    __SELECTED_FEATURE_OP = __VERIFIER_nondet_bool();

	if (validProduct()) {
		o1 = foobar(i1, i2, i3);
		o1 = o1 + o1;
	}
	
	return 0;
}
