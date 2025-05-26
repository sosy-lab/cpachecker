extern unsigned int __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
    unsigned int N = 2 * (__VERIFIER_nondet_uint() % 5);
    int cycle[4] = {1, 2, 3, 4};
    int x = 0;
    unsigned int y = 5;


    y = y % 4;
    while (x < N) {
        if (cycle[y] != (y + 1)) {
            __assert_fail("cycle[y] == y + 1", "example_cycle.c", __LINE__, "main");
        }
        y = (y + 1) % 4;
        x++;
    }


    if (x != N) {
        __assert_fail("x == 4", "example_cycle.c", __LINE__, "main");
    }

    return 0;
}