extern int foo();

int main() {
    int a = 0;
    if (a) {
        foo();
    } else if (a == 1) {
        bar();
    } else if (a == 2) {
        foo();
    } else if (a == 3) {
        bar();
    }
    return 0;
}
