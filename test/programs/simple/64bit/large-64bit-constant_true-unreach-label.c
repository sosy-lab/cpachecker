void *__VERIFIER_nondet_pointer(void);

int main(void) {
	void *ptr;

	ptr = __VERIFIER_nondet_pointer();
	if (!(((unsigned long)ptr) <= 18446744073709547520UL)) {
		if (((unsigned long)ptr >18446744073709547520UL)) {
			if (((unsigned long)ptr) > 18446744073709547520UL) {
				long res = (long)ptr;
				int status = (int)res;
				if (status==0) {
ERROR:
					return 1;
				}
			}
		}
	}
	return 0;
}
