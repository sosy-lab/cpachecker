void main() {
	int p1;
	int p2;

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
