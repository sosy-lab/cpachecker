extern int __VERIFIER_nondet_int();
extern int input();

int __SELECTED_FEATURE_SPL;
int __SELECTED_FEATURE_X;
int __SELECTED_FEATURE_Y;
int __SELECTED_FEATURE_PLUS;
int __SELECTED_FEATURE_MINUS;


int validProduct() {
	if ( __SELECTED_FEATURE_SPL
		&& 	  ((__SELECTED_FEATURE_X && !__SELECTED_FEATURE_Y) 
			|| (!__SELECTED_FEATURE_X && __SELECTED_FEATURE_Y))
		&&    ((__SELECTED_FEATURE_PLUS && !__SELECTED_FEATURE_MINUS) 
		    || (!__SELECTED_FEATURE_PLUS && __SELECTED_FEATURE_MINUS))
	   ){
		return 1;
	}
	return 0;
}

int foobar(int x, int y, int z) {
	int a;
	
	if (__SELECTED_FEATURE_X) {
		G1: a = x;
	} else if (__SELECTED_FEATURE_Y) {
		G2: a = y;
	}
	if (__SELECTED_FEATURE_PLUS) {
		G3: z += a;
	} else if (__SELECTED_FEATURE_MINUS) {
		G4: z -= a;
	}
	return z;
}


int main() {
	int x = input();
	int y = input();
	int z = input();

	__SELECTED_FEATURE_Y = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_X = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_MINUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_PLUS = __VERIFIER_nondet_int();
	__SELECTED_FEATURE_SPL = __VERIFIER_nondet_int();

	if (validProduct()) {
		foobar(x, y, z);
	}

	return 0;
}
