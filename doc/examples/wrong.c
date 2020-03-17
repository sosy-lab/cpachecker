extern void __VERIFIER_error(void);

int f(int a) {
	int l = a + 1;
	l++;
	l = l + a;
	if (l > 2) {
		l++;
	}
	l -= 3;
	return l;
}

int main() {
  int x  = 1;
  int b = 0;
  if (x < 0) {
	b = f(x);
  } else {
	  b = -f(-x);
  }
  if (b > 0 && x == 1) {
  __VERIFIER_error();
  }
  b++; x--;
}
