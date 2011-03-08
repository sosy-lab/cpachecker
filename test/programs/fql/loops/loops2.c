void main() {
	int __BLAST_NONDET;
	int p1;
	int p2;

	p1 = __BLAST_NONDET;
	p2 = __BLAST_NONDET;

	while (0) {
		if (p1) {
			goto M;
		}
	}

M:

	while (0) {
		if (p2) {
			goto E;
		}
	}

E:	return;

}
