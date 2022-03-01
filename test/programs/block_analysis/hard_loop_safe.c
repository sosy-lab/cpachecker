extern int __VERIFIER_nondet_int();

int main() {

    int x = 0; //__VERIFIER_nondet_int();
    int y = 0;

    while (x != 1000) {
        while (y != 1000) {
            y++;
        }
        x++;
    }

    if (x != 1000 || y != 1000)
        ERROR: return -1;

    return 0;

}
