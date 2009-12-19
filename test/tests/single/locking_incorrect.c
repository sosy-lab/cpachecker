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

	// unlocking after init() is not allowed by the automaton
	unlock();

	if (y != 0) {
	ERROR:
			goto ERROR;
		}

	return (0);
}
