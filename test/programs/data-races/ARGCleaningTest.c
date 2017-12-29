// The test checks the work of cleanin BAM caches
int global; 
int global2; 
void h(int a) 
{
    //Uninportant function.
	int b = 0;
    b++;
    if (a > b) {
        b++;
    }
}

void l(int a) 
{
    //Uninportant function.
	int b = 0;
    b++;
    if (a > b) {
        b++;
    }
}

int f(int a) 
{
    //Uninportant function, but there were predicates inserted.
	int b = 0;
    b++;
    if (a > b) {
        b++;
    }
	return b;
}

int g() {
	int p = 0;
    int b;
    h(p);
	b = f(p);
    l(p);
    if (b == 0) {
      //false unsafe. f should be cleaned after refinement.  
	  global++;
    }
    //true unsafe
    global2++;
}

int ldv_main() {
	g();
}
