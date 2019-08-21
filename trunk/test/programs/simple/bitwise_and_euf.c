void f() { }
int main() {
	int a;
	int b;
	int x;
	int y;
	a = 42;
	b = 23;
	x = a & b;
	y = 42;
	y = y & b;
	f(); // force abstraction
	if (x != y) {
ERROR:
		goto ERROR;
	}
}
