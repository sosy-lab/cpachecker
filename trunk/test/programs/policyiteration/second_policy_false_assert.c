#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
    char in[11];
    int i = 0;
    int j = 0;
    int c = 0;
    while (__VERIFIER_nondet_int()) {
        j = c;
        i = i * 10U + j;
        c = in[i];
    }
      if (!(i>=0)) {
          goto ERROR;
      }
      return 0;
    ERROR:
      return -1;
}
