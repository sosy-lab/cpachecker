extern void * __VERIFIER_nondet_pointer();
extern void __VERIFIER_error();

int a = 0;
int *p2 = &a;

int main() {

    int *p = (int*) __VERIFIER_nondet_pointer();
    *p = 5;

    if (a > 0) {
ERROR:
        __VERIFIER_error();
    }
}
