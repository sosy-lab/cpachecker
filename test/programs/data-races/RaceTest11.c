int global; 
int f(int a) 
{
	int b;
	if (b) {
		b++;
	} else {
		g();
	}
	return a;
}

int g() {
	int p = 0;
	f(p);
	if (p) {
		global = 0;
	}
	f(p);
	if (!p) {
		intLock();
		global = 1;
		intUnlock();
	}
}

int ldv_main() {
	g();
}
