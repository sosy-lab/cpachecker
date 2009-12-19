// used to test the ObserverAutomaton "LockingAutomaton"

int locked;

int lock() {
	locked = 1;
	return (0);
}

int unlock() {
	locked = 0;
	return (0);
}

int init() {
	locked = 0;
	return (0);
}

int main() {

	init();

	lock();

	if (y != 1) {
	ERROR:
			goto ERROR;
		}

	unlock();

	if (y != 0) {
		ERROR:
				goto ERROR;
			}

	lock(); // remains locked. This should result in a Warning?

	if (y != 1) {
		ERROR:
				goto ERROR;
			}


	return (0);
}
