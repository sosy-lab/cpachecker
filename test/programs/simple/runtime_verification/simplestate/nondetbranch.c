extern int k;

int entry() {
	k = 0;
	int flag = nondet_int();

	for(int i = 0; i < (flag?1000000:100); i++) {
		if(!flag) {
			k+= 1;
			while(0); //force abstraction
		}
		anti_op();
	}

	return 1;
}
