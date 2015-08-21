extern int __VERIFIER_nondet_int();
extern int input();

int foobar(int x, int y, int z) {
	int a;
	
	if (x < y) {
	    a = x;
	} else {
		a = y;
	}
	
	if ((z - a) < 0) {
		a *= -1;
	}
	z -= a;
	
	return z;		
}


int main() {
	int x = input();
	int y = input();
	int z = input();
	
	foobar(x, y, z);

	return 0;
}