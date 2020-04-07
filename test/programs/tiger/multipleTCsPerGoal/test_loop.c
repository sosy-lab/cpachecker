extern int __VERIFIER_nondet_int();

int foobar(int x) {
	int i = 0;
	while(i < x){
	i++;
	}
	if(i > 10){
	G1: return 1;
}
return 2;
	
}


int main() {
	int x= __VERIFIER_nondet_int();
	foobar(x);
	return 0;
}
