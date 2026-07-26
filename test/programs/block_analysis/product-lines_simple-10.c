void reach_error() { __assert_fail("0","",0,""); }

extern int __VERIFIER_nondet_int(void);

// state
int w = 1, m = 0, p = 0, s = 1;

int main(void) {
  int i = 0, t1, t2, t3, t4;

  while (1) {
    if (i >= 1) {
        return 0;
    }

    if (s) {
      if (!p && w == 1 && !m)
        p = 1;
    }

    if (w == 0 && p)
      reach_error();
    i++;
  }

  return 0;
}

