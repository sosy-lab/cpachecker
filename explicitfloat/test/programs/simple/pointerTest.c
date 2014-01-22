#include <stdlib.h>

struct s { int f; };

void f(struct s *s) {
	if ((*s).f != 1) {
ERROR:
		goto ERROR;
	}
}


void f2(int *p) {
	if ((*p) != 1) {
ERROR:
		goto ERROR;
	}
}

int main() {
	int *p = malloc(sizeof(int));
	(*p) = 1;
	f2(p);

	struct s *s = malloc(sizeof(struct s));
	s->f = 1;
	f(s);

	return 0;
}
