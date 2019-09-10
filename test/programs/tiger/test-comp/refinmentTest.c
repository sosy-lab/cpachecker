extern int __VERIFIER_nondet_int();



int foobar(int x) {
	int a;
	if(x > 5){
	x = 2;
	}else{
	x = 0;
	}

	if (x == 2) {
		G1: a = x + 1;
	} 
	return a;
}


int main() {
	int x = __VERIFIER_nondet_int();

	foobar(x);
	
	return 0;
}
