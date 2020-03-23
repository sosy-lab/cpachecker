extern __VERIFIER_nondet_int();

int random(){
	return __VERIFIER_nondet_int();
}

int processNumber(int d) {
	if(d < 5) return -5;
	return 5;
}

int main(){
	char name[] = {'f', 'l'};
	int useless = random();
	int y[] = {0};
	y[0] = random();
	int x = y[0];
	int bound = processNumber(x);
	if(bound < 0) bound = bound*(-1);
	if(x  > 0)
		if(x < bound)
			if(x > 1)
				if(x != -1 && x != 6)
					goto ERROR;
	if(name[0] == 'f') {
		useless++;
	}

EXIT: return 0;
ERROR: return 1;
}

