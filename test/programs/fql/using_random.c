
extern int rand(int counter);

int myRand() {
	/*
	 * TODO: we need a counter since it is assumed that rand is a pure function
	 */
	static int counter = 0;
	return rand(counter++);
}

int foo() {
	int x;
	int y;
	int z;

	x = myRand();
	y = myRand();
	z = myRand();

	if (x != y && z == 10) {
		return 1;
	}
	else {
		return 0;
	}
}
