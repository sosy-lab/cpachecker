//#include <stdlib.h>

int times2(int x)
{
 return x*2;
}

int negate(int x)
{
return x*-1;
}

void check(int x)
{
	if (x < 0){
		int n = 0;
		int m = rand();
		int p = m;
		if (m < 0) {
			m = m*m;
			m = m/p;
		}
		n = m + 1;
	
		ERROR: return;
	}
	else{
		x += 42;
	}
}

int globalVar;

void main() {

	int a, b;
	int * d = &globalVar;
	void * e = malloc(sizeof(int));
	d = (int*)e;
	a = rand();
	//a = 11;
	b = -7;

	a = times2(a);
	a = times2(a);
	b = negate(b);

	*d = rand();
	*d = negate(*d);

	if (a == 44 && b == 7)
	{
		ERROR: return;
	}

	check(a);
	check(b);

	int c = a + b;
	c *= 2;
}


