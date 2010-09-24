int foo() {
	int x;
	int y;
	int z;
        int __BLAST_NONDET;

	x = __BLAST_NONDET;
	y = __BLAST_NONDET;

	if (x != y) {
		return 1;
	}
	else {
		return 0;
	}
}

