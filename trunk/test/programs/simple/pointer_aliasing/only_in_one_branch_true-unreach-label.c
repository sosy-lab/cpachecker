int i;
int j;

extern int __VERIFIER_nondet_int();

void f(int *p) {
	*p = 1;
	j = 1;
}

int main() {
	if (__VERIFIER_nondet_int()) {
		f(&i);
	}
	if (i != 0 && j == 0) {
ERROR:
		return 1;
	}
}
