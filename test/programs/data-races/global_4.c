/* Recursion test
 * In this test global variable gvar has two access points: write without locks
 * and write under lock kernDispatchDisable.
 */

#line 1 "../test/programs/races/global_4.c"

int gvar;

void f(void) {
	gvar = 1;
}

void h() {
	g();
}

void g() {
	kernDispatchDisable();
	h();
	gvar = 1;
	kernDispatchEnable();
}

void ldv_main(void) {
	f();
	h();
}