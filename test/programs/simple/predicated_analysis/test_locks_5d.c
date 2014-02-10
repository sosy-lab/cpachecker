extern int __VERIFIER_nondet_int();
int main()
{
    int lk1; // lock variable

    int lk2; // lock variable

    int lk3; // lock variable

    int lk4; // lock variable

    int lk5; // lock variable

    int flag = 1;
    int cond;
    int p;

    while(1) {
        cond = __VERIFIER_nondet_int();
        p = __VERIFIER_nondet_int();
        if (cond == 0) {
            goto out;
        } else {}
        lk1 = 0; // initially lock is open

        lk2 = 0; // initially lock is open

        lk3 = 0; // initially lock is open

        lk4 = 0; // initially lock is open

        lk5 = 0; // initially lock is open


    // lock phase
        if (p > 0) {
            lk1 = 1; // acquire lock
        } else {}

        if (p > 1) {
            lk2 = 1; // acquire lock
        } else {}

        if (p > 2) {
            lk3 = 1; // acquire lock
        } else {}

        if (p > 3) {
            lk4 = 1; // acquire lock
        } else {}

        if (p > 4) {
            lk5 = 1; // acquire lock
        } else {}


    // unlock phase
        if (p > 0) {
            flag = lk1;
            lk1 = 0;
        } else {}

        if (p > 1) {
            flag = lk2;
            lk2 = 0;
        } else {}

        if (p > 2) {
            flag = lk3;
            lk3 = 0;
        } else {}

        if (p > 3) {
            flag = lk4;
            lk4 = 0;
        } else {}

        if (p > 4) {
            flag = lk5;
            lk5 = 0;
        } else {}

    }
  out:
    return 0;
  ERROR:
    return 0;  
}

