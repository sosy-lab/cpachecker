extern int __VERIFIER_nondet_int(void);

void main() {
    int data = 0;
    while (__VERIFIER_nondet_int()) {
        data = __VERIFIER_nondet_int();
    }
    if (data) {
        ERROR: goto ERROR;
    }
}
