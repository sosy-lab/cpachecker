int unsafe;
int global;
int (*p)();

int main() {
    p = &g;
    (*p)();
}

int f() {
	unsafe++;
}

int g() {
	global = 1;
}

int ldv_main() {
	f();
    main();
    f();
}

