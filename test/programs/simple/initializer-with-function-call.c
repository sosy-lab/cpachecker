int i = 0;
int f() {
	return 1;
}

int main() {
	int i = f();
	if (i != 1) {
ERROR:		;
	}

	int a[2] = { f(), f() };
}
