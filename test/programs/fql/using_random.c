int input();

int foo() {
	int x;
	int y;
	int z;

	x = input();
	y = input();
	z = input();

	if (x != y && z == 10) {
		return 1;
	}
	else {
		return 0;
	}
}

