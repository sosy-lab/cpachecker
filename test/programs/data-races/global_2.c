#line 1 "../test/programs/races/global_2.c"

extern void kernDispatchDisable() ;
extern void kernDispatchEnable() ;
extern int intLock(void) ;
extern void intUnlock(int level ) ;

int gvar;

int f(void) {
	gvar = 1;
	return 0;
}

void mqSend(void) {
	intLock();
	g();
}

void g(void) {
	mqSend();
}

void ldv_main(void) {
	g();
	gvar = 2;
	kernDispatchDisable();
	gvar = 3;
	g();
	gvar = 4;
	kernDispatchEnable();
	gvar = 5;
}
