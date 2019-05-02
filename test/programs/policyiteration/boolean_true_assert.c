void assert(int cond) { if (!cond) { ERROR: return; } }

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);

const int BOUND = 1000;

int main () {
    int test = __VERIFIER_nondet_int();
    int sum = 0;
    for (int i=0; i<BOUND; i++) {
        if (test) {
            sum++;
        }
    }
    assert(sum == BOUND || sum == 0);
}
