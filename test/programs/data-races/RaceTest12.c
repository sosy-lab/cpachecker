int global; 
int f(int a) 
{
	if (10 & a) {
	  global++;
	}
	return a;
}

int g() {
	int p = 0;
	f(p);
	global++;
}

int ldv_main() {
	g();
}
