extern int __VERIFIER_nondet_int();

int main() {

    int x = __VERIFIER_nondet_int();
    int y = 0;

    while (x != 1000) {
        while (y != 1000) {
            y++;
        }
        x++;
        y--;
    }

    if (y == 1000)
        ERROR: return -1;

    return 0;

}
