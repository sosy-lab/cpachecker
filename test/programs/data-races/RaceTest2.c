int unsafe;
int global;

int main() {
    int undef, tmp;
    if (undef) {
        intLock();
    }
    tmp++;
    if (undef) {
        intUnlock();
    }
}

int f() {
	unsafe++;
}

int ldv_main() {
	f();
    main();
    f();
}

