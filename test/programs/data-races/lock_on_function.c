int unsafe;
int unknown;

int intLock() {
    unsafe = 1;
}

int ldv_main() {
	unsafe = 0;
	intLock();
}
