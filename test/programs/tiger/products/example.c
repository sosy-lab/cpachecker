extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a = 0;
	int o = 0;
	
	if (x < y) {
		G1: a = x;
	} else {
		if (x < y) {
			G2: a = x;
		}
		G3: a = y;
	}
	
	if (z > 10) {
		if ((z - a) < 0) {
			G4: a *= -1;
		}
		G5: o -= a;
	}
	
	return o;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int z = __VERIFIER_nondet_int();

	int o = foobar(x, y, z);
	
	printf("%i", o);
	
	return 0;
}
