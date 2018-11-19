int __VERIFIER_nondet_int();
int main() {
    int i = __VERIFIER_nondet_int();
    if (i) {
        if (i > 5) {
            i = i + 5;
            return 0;
        } else {
            i = i + 1;
            return 0;
        }
    } else {
        i = i - 2;
        i = i - 3;
        return 0;
    }
}
