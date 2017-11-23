extern int __VERIFIER_nondet_int();

int foobar(int x, int y) {
	G1: int a = 0;

	if (x < y) {
		G2: a = x;
	} else{
		G3: a = y;
	}
	return a;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();

	int tmp = foobar(x, y);
	
	return 0;
}
