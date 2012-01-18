void f() { };

void main() {
	int status;
	int tmp;
	tmp = nondet_int();
	int x;
	if (tmp) {
		status = -1073741823L;
		x = 0;
	} else {
		status = 0;
	}

	f();

	if (status < 0L) {
ERROR:
		goto ERROR;
	} else {
	}
}
