extern int __VERIFIER_nondet_int();

int main() {

	int i = __VERIFIER_nondet_int();
	if (i == 0) {
		// Trigger UNSAFE_DEREFERENCE
		int* j = 0;
		*j = 6;
	} else if (i == 1) {
		// Trigger Memory Leak
		int *a;
		int *b;
		a = malloc(8);
		b = malloc(8);
		a = b;
		free(a);
	} else if (i == 2) {
		// Trigger potentially unsafe dereference
		int *a;
		int size = sizeof(int);
		a = malloc(size);
		*a = 5;
	} else if (i == 3) {
		//trigger INVALID FREE
		int *ptr;
		free(ptr);
	}  else if (i == 4) {
		// trigger DOUBLE_FREE
	    int *a;
	    int *b;
	    b=malloc(8);
	    a = b;
	    free(b);
	    free(a);
	}

	return (0);
	ERROR: return (-1);
}
