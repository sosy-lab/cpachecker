extern unsigned int __VERIFIER_nondet_uint();

int foobar(unsigned int x) {
	int i = 0;

	while(i < 5){
		i = i+x;
	}
	
G1: return i;

}


int main() {
	unsigned int x= __VERIFIER_nondet_uint();
	foobar(x);
	return 0;
}
