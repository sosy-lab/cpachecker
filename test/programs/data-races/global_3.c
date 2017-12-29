#line 1 "../test/programs/races/global_3.c"

extern void kernDispatchDisable() ;
extern void kernDispatchEnable() ;
extern int intLock(void) ;
extern void intUnlock(int level ) ;

int gvar;

int f(void) {
	gvar = 1;
	return 0;
}

//Annotated function, free intLock
void ldbBreakpoint(void) {
	intLock();
	f();
}

void g(void) {
	int b;
	kernDispatchDisable();
	if(b)
		kernDispatchEnable();
	else 
		b = b+5;//special instruction to pospone call to mqSend
	ldbBreakpoint();
	//race should be detected as far as gvar can be accessed
	// with kernDispatchDisable and without it
	gvar = 10;
}

void ldv_main(void) {
	g();
}
