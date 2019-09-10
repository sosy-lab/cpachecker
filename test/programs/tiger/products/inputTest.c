extern int __VERIFIER_nondet_int();

int foobar(int x, int y) {
	int a;
	
	if (x < 2) {
		G1: a = x;
		G5: a = a + 1;
	} else {
		if(y > 2){
		G2: a = x * -1;
		} else{
		a = a;
		}
	}
	G6: return a;
}


int main() {
	int x= __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();

	foobar(x, y);
	
	return 0;
}
