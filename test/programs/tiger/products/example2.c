extern int __VERIFIER_nondet_int();

int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	int a;
	
	if (x < y) {
		G1: a = x;
	} else {
		if (x < y) {
			G2: a = x;
		}
		
	}
	
	return a;
}
