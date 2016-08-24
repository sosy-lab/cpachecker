extern void __assert_fail();

int main() {
	int x1 = 0;
	int x2 = 0;
	int x3 = 0;
	int x4 = 0;
	int x5 = 0;

	int i = 1;
	while (1) {
		if (i == 1) {
			x1++;
		} else if (i == 2) {
			x2++;
		} else if (i == 3) {
			x3++;
		} else if (i == 4) {
			x4++;
		} else if (i == 5) {
			x5++;
		}

		i++;
		if (i == 6) {
			i = 1;
			if (!(x1 == x2 && x2 == x3 && x3 == x4 && x4 == x5)) {
				__assert_fail();
				return 1;
			}
		}
	}
}
