extern int __VERIFIER_nondet_int();

int foobar(int x) {
	int a;
	if (x < 2) {
		a = 1;
	} else{
		a = 2;
	}
	G1: return a;
}


int main() {
	int x= __VERIFIER_nondet_int();
	foobar(x);
	return 0;
}
