extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint();
extern int __VERIFIER_nondet_int();
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern _Bool __VERIFIER_nondet_bool();

int main() {
    int i = 0;
    int j = 0;
    int k = 0;
    int d = 0;
    while (__VERIFIER_nondet_int()) {
        if (__VERIFIER_nondet_int()) {
            k = 1;
            while (__VERIFIER_nondet_int()) {
                if (j == 0) {
                    j = 1;
                } else if (j == 1) {
                    j = 2;
                } else if (j = 2) {
                    j = 3;
                } else if (j = 3) {
                    j = 4;
                }
                k++;
            }
        } else {
            d++;
            k = -1;

        }
        if (i == 0) {
            i = 1;
        } else if (i == 1) {
            i = 2;
        } else if (i == 2) {
            i = 3;
        } else if (i == 3) {
            i = 4;
        }
    }
    __VERIFIER_assert(i >= 0 && i <= 8);
    __VERIFIER_assert(j >= 0 && j <= 4);
    __VERIFIER_assert(k >= -1);
    __VERIFIER_assert(d >= 0);
}
