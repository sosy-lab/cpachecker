
extern int rand();

int foo() {
	int x;
	int y;
	int z;

	x = rand();
	y = rand();

	if (x != y) {
		return 1;
	}
	else {
		return 0;
	}
}
