// this test case checks wether struct copies are handled
struct s { int x; };
int main() {
	struct s a;
	struct s b;
	a.x = 4;
	b.x = 8;
	a = b;
	if (a.x != b.x) {
ERROR:
		goto ERROR;
	}
}
