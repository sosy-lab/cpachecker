void init() {
	anti_op();
}

void lock() {
	anti_op();
}

void unlock() {
	anti_op();
}

int entry() {
	init();

	lock();
	unlock();

	lock();
	unlock();

	int lastLock = 0;
	
	lock();
	for(int i = 1; i < 1000; i++) {
		if(i - lastLock == 2 && i < 999) {
			lastLock += 2;
			lock();
		}
		else {
			unlock();
		}
	}

	return 1;
}
