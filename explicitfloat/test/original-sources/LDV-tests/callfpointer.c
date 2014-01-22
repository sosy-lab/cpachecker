/*
system is unsafe, but blast reports it is safe
*/
int VERDICT_UNSAFE;
int CURRENTLY_SAFE;

void f(void g(int)) {
	g(1);
}

void h(int i) {
	if(i==1) {
		ERROR: goto ERROR;
	} else {
		//ok
	}
}
int main(void) {
	f(h);
	//h(0);
	return 0;
}
