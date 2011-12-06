void init() {

}

void lock() {

}

void unlock() {

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
