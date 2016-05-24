extern int __VERIFIER_nondet_int(void);

int flag = 0;

int main() {
	int a = __VERIFIER_nondet_int();
	int b = __VERIFIER_nondet_int();
	int c = __VERIFIER_nondet_int();
	int d = __VERIFIER_nondet_int();
	int id, utriag, ltriag, triag, unknown;
	id = utriag = ltriag = triag = unknown = 0; 
	if(c == 0 || b == 0) {
		triag = 1;
	}
	
	if(triag > 0) {
		if(c == 0 && b == 0) {
			if(a == 1 && d == 1) {
				id = 1;
			}
			ltriag = 1;
			utriag = 1;
		}
		else if(b == 0) {
			ltriag = 1;
		}
		else {
			utriag = 1;
			flag = c;
		}
	}
	else {
		unknown = 1;
	}

	if(triag) {
		if(!(id || ltriag || utriag)) {
			flag = 1;
		}
	}

	if(triag && utriag == ltriag) {
	    flag = c;
	    flag = b;
	}

	if(!(unknown > 0 || triag > 0))
	{
		flag = 1;
	}
	return 1;
}
