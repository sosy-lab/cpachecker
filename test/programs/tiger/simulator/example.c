extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a = 0;
	
	if (x < y) {
		G1: a = x;
	} else {
		if (x < y) {
			G2: a = x;
		}
		G3: a = y;
	}
	
	if (z < y) {
		if (z < x) {
			G4: a = y;
		}
		G5: z = a;
	}
	
	G6: return z;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int z = __VERIFIER_nondet_int();

	int tmp = foobar(x, y, z);
	int o = tmp;
	
	printf("%i", o);
	
	return 0;
}
