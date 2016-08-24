int f() {
	int ret;
	return ret;
}

int main() {
	int x = f();
	int y = f();
	// x and y may be equal here, so this error location has to be reachable
	if (x != y) {
ERROR:
		goto ERROR;
	}
}
