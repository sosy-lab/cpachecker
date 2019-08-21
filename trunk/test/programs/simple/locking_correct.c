// used to test the ObserverAutomaton "LockingAutomaton"

int locked;
int x;

int lock() {
	locked = 1;
	return 0;
}

int unlock() {
	locked = 0;
	return 0;
}

int init() {
	locked = 0;
	return 0;
}

int main() {
	int myId = 10;

	init();

	lock();

	x = 0;

	unlock();

	lock(); // remains locked. This should result in a Warning?

	x  = /* comment */  x +   1 ;

	return (0);
}
