extern int __VERIFIER_nondet_int();

int foobar(int x) {
	int a;
	
	if (x > 2) {
		G1: a = x;
	} else {
		G2: a = 0;
	}

	if(a == 0){
		G3: a = 5;
	}else{
		G4: a = 0;
	}

	return a;
}


int main() {
	int x= __VERIFIER_nondet_int();

	foobar(x);
	
	return 0;
}
