extern int __VERIFIER_nondet_int();
extern int input();

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
	int x = input();
	int y = input();
	int z = input();

	foobar(x, y, z);
	
	return 0;
}
