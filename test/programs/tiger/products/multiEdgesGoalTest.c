extern int __VERIFIER_nondet_int();

int foobar(int x, int y) {
	int a;
	
	if (x < 2) {
		G1: a = x;
	} else {
		G2: a = x * -1;
	}
	
	if (y > 10) {
		G3: a *= -1;
	}else{
		G4: a = 5;
	}

	
	return a;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();

	foobar(x, y);
	
	return 0;
}
