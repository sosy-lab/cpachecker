// The test checks the work path iterator
int global; 
int global2; 
void h(int a) 
{
    //access to global is false
    if (a) {
	  global = 1;
    }
}

void l(int a) 
{
    //The first call
    h(1);
}

int f(int a) 
{
    //the second call
    h(a);
}

int k(int a) {
    //one more function call
    h(a);
}

int g() {
	int p = 0;
    f(p);
	intLock();
    global = 2;
    intUnlock();
    l(p);
    k(p);
}

int ldv_main() {
	g();
}
