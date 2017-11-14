extern int __VERIFIER_nondet_int();

int foobar(int x, int y, int z) {
	int a = 0;

	if (x < y) {
		if(y < z){
			G1: a = x + y;
			if(a < z)
				G2: a = a + z;
		}
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
