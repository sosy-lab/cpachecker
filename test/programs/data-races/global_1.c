extern void kernDispatchDisable() ;
extern void kernDispatchEnable() ;
extern int intLock(void) ;
extern void intUnlock(int level ) ;

int gvar;

int f(void) {
	gvar = 1;
	return 0;
}

void g(void) {
	int b;
	kernDispatchDisable();
	b = gvar;
	kernDispatchEnable();
}

void ldv_main(void) {
	f();
	g();
	return;
}
