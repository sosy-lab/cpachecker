//Tests checks shared merge work

struct M {
  int* y;
} var;

void g(struct M* tmp) {
	tmp->y = 0;
}

void ldv_main(void) {
	g(0);
    g(&var);
}
