extern int __VERIFIER_nondet_int();

int main() {
	int x = __VERIFIER_nondet_int();
	if (x <= 0)
		goto EXIT;
	int answer = 0;
	while (1) {
		x -= 2;
		if (x < 0) {
			x += 2;
			if (x == 1) {
				answer = 1;	
			}
			break;
		}
	}

	if (x % 2 == 0 && answer == 0) {
		goto ERROR;
	}
	
EXIT:
	return 0;
ERROR:
	return 1;
}
