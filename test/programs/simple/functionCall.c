int f() {
	int x;
	x = 0;
	return (x);
}

int main() {
	int y;
	y = f();
	if (y != 0) {
ERROR:
		goto ERROR;
	}
	return (y);
}
