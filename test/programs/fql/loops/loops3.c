void foo() { }

void main() {
	int __BLAST_NONDET;
	int p;
	int x;

	p = __BLAST_NONDET;
	x = __BLAST_NONDET;

	while(1) { // outer loop
		foo();

		if (p) {
			goto loop1;
		} else {
			goto loop2;
		}

loop1:
		foo();
loop2:
		foo();

		if (x) {
			goto loopexit;
		}

		if (p) {
			goto loop1;
		} else {
			goto loop2;
		}
loopexit:
		foo();
	}
}
