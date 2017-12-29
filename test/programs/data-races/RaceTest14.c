int false_unsafe;
int global;

int main() {
    global++;
}

int f() {
    int i = 0;
	kernDispatchDisable();
    i++;
    intLock();
    g();
}

int g() {
    global++;
}

int ldv_main() {
    main();
	f();
}

