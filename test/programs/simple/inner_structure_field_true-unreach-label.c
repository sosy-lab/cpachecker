
struct inner {
	int f;
};

struct outer {
	struct inner inner;
};

int main(void)
{
	struct outer outer, outer1;
	struct outer *pouter = &outer, *pouter1 = &outer1;
	pouter->inner.f = 0;
	pouter1->inner.f = 0;
	if (outer.inner.f != 0) {
		ERROR: return 1;
	} else {
		return 0;
	}
}
