void main() {
	int __BLAST_NONDET;
	int x;

	x = __BLAST_NONDET;

L1:
	if (x) {
		goto L2;
	} else {
		goto L3;
	}

L2:
	if (x) {
		goto L1;
	} else {
		goto L3;
	}

L3:
	if (x) {
		goto L1;
	} else {
		goto L2;
	}
}

