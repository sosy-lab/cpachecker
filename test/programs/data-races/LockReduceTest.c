/* Test check the reduction of locks */

int gvar;
int threadDispatchLevel = 0;

int f(void) {
    gvar = 0;
    intUnlock();
    threadLock();
    splbio();
	return 0;
}

void ldv_main(void) {
    intLock();
    intLock();
	threadLock();
    //(2,1,0) -> (1,1,0)
	f();
    //(0,2,1) -> (1,2,1)
    //(1,2,1) -> (1,1,1)
	f();
    //(0,2,2) -> (0,3,3)
    threadUnlock();
    threadUnlock();
    threadUnlock();
    //(0,0,3) -> (0,0,1)
    f();
    //(0,1,2) -> (0,1,4)
}
