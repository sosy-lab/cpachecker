
void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

void testLateAssignment() {
	int a;
	int* q;

	q = &a;
	a = 5;
	test(*q == a);
}

void testIndirection() {
	int a;
	int b;
	int* p1;
	int* p2;

	a = 5;
	b = 10;
	p1 = &a;
	p2 = p1;

	test(*p1 == *p2);
	test(*p2 == a);
	test(*p1 != b);

	a = b;
	test(*p1 == b);
}


void testIf() {
	int a;
	int b;
	int* p;
	a = 13;
	b = 23;
	if (a < b) {
		p = &a;
	} else {
		p = &b;
	}
	
	test(*p == 13);
}

void testInit() {
	int a = 10;
	int* p = &a;
	test(*p == 10);
}


void testPA() {
	int a;
	int* p;
	
	p = &a;
	a = 65;
	*((int *) p) = (int *) 50;

	test(a == 50);
}

void main() {
	testIndirection();
	testLateAssignment();
	testIf();
	testInit();
	testPA();
}
