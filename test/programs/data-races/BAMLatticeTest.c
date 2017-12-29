/* Test check the lattice with influence of BAM */

int gvar;

int f(void) {
	gvar = 1;
	return 0;
}

void ldv_main(void) {
	f();
	kernDispatchDisable();
	f();
}
