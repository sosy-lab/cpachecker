int foo(int a) {
	return a;
};

extern typeof(foo) foo2;

int foo2(int);

void main() {}
