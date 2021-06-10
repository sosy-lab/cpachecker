extern int getchar();
extern void printf(char*, int);

int main(void){
	int a, b, c, d;						// T(*) = U
	a = getchar();						// T(a) = T
	b = 10;                 			// T(b) = U
	c = a;                 				// T(c) = T
	d = b;								// T(d) = U
	c = 2;								// T(c) = U
	printf("%d", a);					// T(a) = T
	printf("%d", a);					// T(b) = U
	return 0;
}
