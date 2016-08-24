void foo() { }

void main() {
	int p;
	int x;

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
