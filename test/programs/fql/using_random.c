int foo() {
	int x;
	int y;
	int z;
	int __BLAST_NONDET;

	x = __BLAST_NONDET;
	y = __BLAST_NONDET;
	z = __BLAST_NONDET;

	if (x != y && z == 10) {
		return 1;
	}
	else {
		return 0;
	}
}

