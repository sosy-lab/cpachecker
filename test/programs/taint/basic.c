extern int getchar();
extern void printf(int);

int main(void){
	int a, b, c, d;						// T(*) = U
	a = getchar();						// T(a) = T
	b = 10;                 			// T(b) = U
	c = a;                 				// T(c) = T
	d = b;								// T(d) = U
	c = 2;								// T(c) = U
	printf(a);							// T(a) = T
	printf(b);							// T(b) = U
	return 0;
}
