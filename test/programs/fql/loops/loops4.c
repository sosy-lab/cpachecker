void main() {
	int __BLAST_NONDET;
	int x;

	x = __BLAST_NONDET;

	if (x) {
		goto L1;
	} else {
		goto L2;
	}

L1:
	if (x) {
		goto L2;
	} else {
		goto end;
	}

L2:
	if (x) {
		goto L1;
	} else {
		goto end;
	}

end:
	;
}
