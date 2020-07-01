extern void __assert_fail();

int main() {
	int N;
	int x = 0;
	int i = 0;

  	while (i < N) {
	  	if (!(x == 0)) {
			__assert_fail();
			return 1;
	  	}
   		i = i+1;
  	}
  	if (!(x == 0)) {
		__assert_fail();
		return 1;
  	}
}
