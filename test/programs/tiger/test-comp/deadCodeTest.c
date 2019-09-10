extern int __VERIFIER_nondet_int();



int foobar(int x) {
	int a;
	x = 0;
	if (x > 2) {
		G1: a = x;
	} 
	return a;
}


int main() {
	int x = __VERIFIER_nondet_int();

	foobar(x);
	
	return 0;
}
