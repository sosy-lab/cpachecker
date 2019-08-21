// The error location is reachable in this file, because x is used when uninitialized and so both paths in f have to be possible on each call.

int i = 0;

void f() {
	int x;
	i++;
	if (x == 0) {
		x = 0;
	} else {
		if (i == 2) {
			x = 2;
		} else {
			x = 1;
		}
	}
}

int main() {
	f();
	f();
	if (i == 2) {
ERROR:
		goto ERROR;
	}
}
