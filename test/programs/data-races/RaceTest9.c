int global;


int f() {
	int local = 0;
	local++;
	intLock();
}

int g() {
	f();
}

int m() {
	kernDispatchDisable();
	g();
	kernDispatchEnable();
}

int h() {
	kernDispatchDisable();
	f();
	//global++;
	kernDispatchEnable();
}

int h1() {
	h();
	global++;
}

int ldv_main() {
	global++;
	m();
	intUnlock();
	h();
	intUnlock();
	h1();
}
