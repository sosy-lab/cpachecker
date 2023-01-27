extern int __VERIFIER_nondet_int();


int simple_function(int a) {
	return a + 1;
}

int main() {
	int x = __VERIFIER_nondet_int();
	int result = x;
	if (x) {
		result = 0;
	} else {
		result = simple_function(result);
	}
	result = simple_function(result);
	if (!(result == 1 || result == 2)) {
		return 0;
	}
	ERROR: return 1;
}
