extern void __assert_fail();

int main() {
	int x = 0;
	while (1) {
		x += 1;
		if (x == 2) {
			x = 0;
		}
		if (x >= 2) {
			__assert_fail();
			return 1;
		}
	}
	return 0;
}
