extern int __VERIFIER_nondet_int();

int foobar(int x, int y) {
	int a;
	if (x < 2) {
		a = 1;
	} else{
	a = 2;	
	}

 	if (y < 5){
		a += 2;
	}else{
		a += 3;
	}
	G1: return a;
}


int main() {
	int x = __VERIFIER_nondet_int();
	int y = __VERIFIER_nondet_int();
	foobar(x, y);
	return 0;
}
