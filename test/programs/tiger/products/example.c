extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a;
	
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
		G5: z -= a;
	}
	
	return z;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int z = __VERIFIER_nondet_int();

	foobar(x, y, z);
	
	return 0;
}
