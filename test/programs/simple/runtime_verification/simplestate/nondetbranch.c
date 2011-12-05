extern int k;

int entry() {
	k = 0;

	if(nondet_int()) {
		for(int i = 0; i < 1000000; i++) {
			anti_op();			
		}
	} else {
		for(int j = 0; j < 100; j++) {
			k += 1;			
		}
	}
	return 1;
}
