void init();
void lock();
void unlock();

int entry() {
	init();

	lock();
	unlock();

	lock();
	unlock();

	int lastLock = 0;
    int n;
	lock();
	for(int i = 1; i < n; i++) {
		if(i - lastLock == 2) {
			lastLock += 2;
			lock();
		}
		else {
			unlock();
		}
	}

	return 1;
}
