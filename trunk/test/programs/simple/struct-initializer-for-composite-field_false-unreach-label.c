struct s {
	int x;
};
struct t {
	struct s s;
};
int main() {
	const struct s s = { .x = 1 };
	struct t t = { .s = s };
ERROR:
	if (t.s.x == 1) {
		return 0;
	} else {
		return 1;
	}
}
