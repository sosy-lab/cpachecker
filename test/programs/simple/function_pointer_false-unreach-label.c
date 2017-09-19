void foo(int i) {
}

void bar(int i) {
}

void baz(int i) {
ERROR:
	i += 3;
}

void (* fncArr[3])(int);

int main(int args, char** argv) {
	int y = 5;
	fncArr[0] = &bar;
	fncArr[1] = &foo;
	fncArr[2] = &baz;
	void (*p)(int) = &baz;

	int i = 0;
	for(; i < 3; i++) {
		fncArr[i](y);
	}

	float x = 5.4;
	p(x);

	return 0;
}


