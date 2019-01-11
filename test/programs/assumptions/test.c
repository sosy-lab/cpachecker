extern int __VERIFIER_nondet();

int main() {

    int a = __VERIFIER_nondet();
    int b;
    if(a >= 0) {
        b = a;
    } else {
        b = a+1;
    }
    if(b < a) {
        ERROR: return -1;
    }

    return 0;
}