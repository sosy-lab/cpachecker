/*extern int __VERIFIER_nondet_int();

int random(){
	return __VERIFIER_nondet_int();
}

int processNumber(int d) {
	if(d < random()) return -1;
	return 1;
}

int main(){
	char name[] = {'f', 'l'};
	int useless = random();
	int y[] = {0};
	y[0] = random();
	int x = y[5];
	int five = processNumber(x);
	five = processNumber(x)*processNumber(x)*5;	
	if(x  > 0)
		if(x < five)
			if(x > 1)
				if(x != -1 && x != 6)
					goto ERROR;
	if(name[0] == 'f') {
		useless++;
	}

EXIT: return 0;
ERROR: return 1;
}
*/
extern int __VERIFIER_nondet_int();


int main(){
	int y = 1;
	int x = y * __VERIFIER_nondet_int();
	if(x > 0)
		if(x < 5)
			if(x > 1)
				if(x != -1 && x != 6)
					goto ERROR;
EXIT: return 0;
ERROR: return 1;
}

/*
allFormulas = []
for(int i = 0; i < n-1; i++){
	oldForm = true;
	for(int j = 0; j < i; j++) {
		bmgr.and(oldForm, edge.get(j))
	}
	allFormulas.add(bmgr.and(oldForm, edge.get(n-1)) //=error location
}
*/
