extern int __VERIFIER_nondet_int();

int main() {

    int x = 0;
    x++;
    x++;
    if ( x == 2) {
        x++;
        x++;
    } else {
        x++;
        x++;
    }
    x--;
    x--;

    if (x != 2) {
        goto ERROR;
    }
    return 0;
    ERROR: return 1;
}
