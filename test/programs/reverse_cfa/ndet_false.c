extern int __VERIFIER_nondet_int();

extern void reach_error();

int main() {
    int x = __VERIFIER_nondet_int() % 2;
    if (x == 1) reach_error();
}