extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "overflow_1-2.c", 3, "reach_error"); }

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

// Was modified by me to test Loop Summary, do not Push under any circumstance

int main(void) {
  unsigned int x = 0;
  unsigned int y = 0;

  while (x < 10000) {
    x += 5;
    y += 1;
  }

  __VERIFIER_assert(x % 2 == 0);
}
