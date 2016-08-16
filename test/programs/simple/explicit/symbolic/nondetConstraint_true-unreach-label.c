extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
	int b = 0;

	if (a > 5 && a > 10) {
		b = 1;		
	} else {
		a = 0;
	}

	if (b) {
		a = a - 10;
	}

	if (a < 0) {
ERROR:
		return -1;
	}
}
