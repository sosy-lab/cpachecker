extern int __VERIFIER_nondet_int();

extern void reach_error();

int main() {
    int x = __VERIFIER_nondet_int() % 2;
    if (x == 2) reach_error();
}