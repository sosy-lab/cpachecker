extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
   unsigned int x = 2 * (__VERIFIER_nondet_uint() % 5);
   //int x = 8;
   int y = 0;

    if (x == 0) {
       y = 1;
    }

    while (x > 0) {
        y++;
        if (x % 2 == 0) {
            x = x - 2 ;
        } else {
            __assert_fail("odd", "example_notequal.c", 19, "main");
        }

    }

    if (y < 1) {
      __assert_fail("kinduction fail", "example_notequal.c", 25, "main");
     }

    return 0;
}
