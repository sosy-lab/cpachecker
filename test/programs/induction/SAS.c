extern void __assert_fail();

int main() {
	int N;
	int x = 0;
	int a = 1;
	int b = 2;
	int c = 3;
	int i = 0;
	int temp;

  	while (i < N) {
  		if (a == b) {
			__assert_fail();
			return 1;
		}
   		temp = a;
   		a = b;
   		b = c;
   		c = temp;
   		i = i+1;
  	}
  	if (!(x == 0)) {
		__assert_fail();
		return 1;
  	}
}
