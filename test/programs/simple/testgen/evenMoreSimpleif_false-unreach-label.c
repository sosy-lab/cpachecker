extern int __VERIFIER_nondet_int();
int a;
int b;
int main() {
	a = __VERIFIER_nondet_int();
	if(a > 6) {
		b = 5;
	} else {
		b = 4;
	}
	if(a < 6) {
		b = 4;
	} else {
		b = 5;
	}

	if (a == 99) {
		return 33;
	}

	if(b == __VERIFIER_nondet_int()) {
		//return 1;
		ERROR: goto ERROR;
	}else{
		return 0;
		//ERROR: goto ERROR;
	}
	return 1;
}
