extern void __assert_fail();

int main() {
	int x1 = 0;
	int x2 = 0;

	int i = 1;
	while (1) {
		if (i == 1) {
			x1++;
		} else if (i == 2) {
			x2++;
		}

		i++;
		if (i == 3) {
			i = 1;
			if (!(x1 == x2)) {
				__assert_fail();
				return 1;
			}
		}
	}
}
