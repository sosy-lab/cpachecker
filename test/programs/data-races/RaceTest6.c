#line 1 "/home/alpha/git/cpachecker/test/cil-files/RaceTest6.c"
int global;

int print() {
	if (global % 2 == 0) {
		printf("global is even: %d", global);
    } else {
		printf("global is odd: %d", global);
	}
}

int increase() {
	lock();
	global++;
	unlock();
}

int ldv_main() {
	switch(undef_int()) {
		case 0:
			print();
			break;
		
		case 1:
			increase();
			break;
    }
}

