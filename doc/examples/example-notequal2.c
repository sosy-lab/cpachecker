extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

extern unsigned __VERIFIER_nondet_uint();

int main() {
    unsigned int x = 2 * (__VERIFIER_nondet_uint() % 11);

    while (x > 0) {
        if (x % 2 == 0) {
            x = x-1 ;
        } else {
            x = x+1;
        }
    }
}


////////////////////////////////////////////////////////////////////////
extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

extern unsigned __VERIFIER_nondet_uint();

int main() {
    unsigned int x = 2 * (__VERIFIER_nondet_uint() % 5);
    int i = 0;

    while (x > 0) {
        if (x % 2 == 0) {
            x = x-2 ;
        } else {
            __assert_fail("oddd", "example_notequal.c", 12, "main");
        }
    }
    if (x < 0) {
      __assert_fail("x < 0", "example_notequal.c", 15, "main");
    }

     if (i < 1) {
      __assert_fail("kinduction fail", "example_notequal.c", 20, "main");
      }

    return 0;
}


////////////////////////////////////////////////////////////////////////
extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
unsigned int line, const char *function);

int main() {
unsigned int N = 2 * (__VERIFIER_nondet_uint() % 2);
int i = 0;
long double x = 2;

while (i < N) {
  x = (2 * x) - 1;
  i++;
}

if (i != N) {
   __assert_fail("i == N", "example_notequal.c", 12, "main");
}
if (x < 0) {
  __assert_fail("x < 0", "example_notequal.c", 15, "main");
}

return 0;
}


////////////////////////////////////////////////////////////////////////
extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

extern unsigned __VERIFIER_nondet_uint();

int main() {
   unsigned int x = 2 * (__VERIFIER_nondet_uint() % 5);
   int y = 0;

    if (x == 0) {
       y = 1;
    }

    while (x > 0) {
        y++;
        if (x % 2 == 0) {
            x = x - 2 ;
        } else {
            __assert_fail("odd", "example_notequal.c", 12, "main");
        }

    }

    if (y < 1) {
      __assert_fail("kinduction fail", "example_notequal.c", 25, "main");
     }

    return 0;
}


////////////////////////////////////////////////////////////////////////

extern unsigned __VERIFIER_nondet_uint();
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
    int x = 20;
    int y = 2;

    if (x - y < 0) {
        __assert_fail("x - y >= 0", "ERROR : Ranking Relation", 10, "main");
    }

    while (x != y) {
        y = y + 1;

        if (y > x) {
            __assert_fail("y <= x", "RANKING RELATION", 17, "main");
        }
    }

    if (x != y) {
        __assert_fail("x == y", "example.c", 22, "main");
    }

    return 0;
}

///////////////////////////////////////////////////////////////////////////////////////////5,5
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
//////////////////////////////////




int main() {
    int x = 2000;
    int y = 10;
///////x나 y의 변수의 크기에 따라 kinduction의 k가 달라짐 16/17


    if (x - y < 0) {
        __assert_fail("x - y >= 0", "ERROR : Ranking Relation", 10, "main");
    }

    while (x != y) {
        y = y+10;

        if (y > x) {
            __assert_fail("y <= x", "RANKING RELATION", 17, "main");
        }
    }

    if (x != y) {
        __assert_fail("x == y", "example.c", 22, "main");
    }

    return 0;
}

