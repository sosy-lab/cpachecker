extern void __VERIFIER_error();

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
    __VERIFIER_error();
    return 1;
	}
	return (y);
}
