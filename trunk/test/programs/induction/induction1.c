extern void __assert_fail();

int main() {
	int x = 0, y = 0;
	while (1) {
		x++;
		y++;
		if (x != y) {
			__assert_fail();
			return 1;
		}
	}
	return 0;
}
