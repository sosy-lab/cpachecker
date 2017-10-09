extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a = 0;
	int b = 0;

	if (x < y) {
		G1: a = x;
		G2: b = y;
	} 
	
	int z = a + b;

	return z;
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
