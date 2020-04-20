struct s {
	int x;
};

struct t {
	void *p;
};

struct t t = { (void *) & (struct s) { .x = 42 } };

int main() {
	if (((struct s *)(t.p))->x != 42) {
ERROR:
		return 1;
	}
	return 0;
}
