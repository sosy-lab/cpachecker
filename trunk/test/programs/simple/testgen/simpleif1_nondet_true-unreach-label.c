/*
 * Nondet: use a function defined in 
 * cpa.predicate.nondetFunctions or cpa.predicate.nondetFunctionsRegexp 
 * like random() or __VERIFIER_nondet_int()
 * simply declaring a variable but never assign something shold work, too
 * 
 */

int main() {
	int a = random();
	int b = 6;
	int c = 6;
	int d = 8;

	if(a == 6) {
		a = 7;
	} else {
		c = 4;
	}

	if(c == 5) {
				
	}

	if (a == 99) {
		return 33;
	}

	if(!(a == 6 && b == 6 && c == 6 && d == 6 )) {
		a = 33;
	} else {
		ERROR: goto ERROR;
	}
	return 1;
}
