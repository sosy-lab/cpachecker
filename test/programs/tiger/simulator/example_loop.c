extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a = 0;

	while (x < y) {
		G1: a = a + 1;
		x = x + 1;
	} 
	
	int z = a;

	return z;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int z = __VERIFIER_nondet_int();

	int tmp = foobar(x, y, z);
	
	return 0;
}
