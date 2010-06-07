
extern int rand(int counter);

int counter = 0;

int myRand() {
	return rand(counter++);
}

int foo() {
	int x;
	int y;

	x = myRand();
	y = myRand();

	if (x != y) {
		return 1;
	}
	else {
		return 0;
	}
}
