int f(int x) {
	if (x) {
		return 1;
	} else {
		return 0;
	}
}

void main() {
	int a = 0;
	if (a) {
		f(a);
	}

	int b;
	b = f(1);
	if (b) {
ERROR:		goto ERROR;
	}
}
