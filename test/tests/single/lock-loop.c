int L = 0;

void lock() {
	if (L != 0) {
ERROR:
		goto ERROR;
	}
	L++;
}

void unlock() {
	if (L != 1) {
ERROR:
		goto ERROR;
	}
	L--;
}

int main() {
	int old, new;
	int undet;
	do {
		lock();
		old = new;
		if (undet) {
			unlock();
			new++;
		}
	} while (new != old);
}
