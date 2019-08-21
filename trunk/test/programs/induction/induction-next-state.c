extern void __assert_fail();

int main() {
	int x1 = 0;
	int x2 = 0;

	int i = 1;
	int nextI;
	while (1) {
		if (i == 1) {
			x1++;
			i = 2;
		} else if (i == 2) {
			x2++;
			i = 3;
			nextI = 4;
		} else if (i == 3) {
			i = 5;
			nextI = 4;
		} else if (i == 4) {
			i = 5;
			nextI = 6;
		} else if (i == 5) {
			i = nextI;
		}

		if (i == 6) {
			i = 1;
			if (!(x1 == x2)) {
				__assert_fail();
				return 1;
			}
		}
	}
}
