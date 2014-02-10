extern int __VERIFIER_nondet_int();
int main()
{
    int lk1; // lock variable

    int lk2; // lock variable

    int lk3; // lock variable

    int lk4; // lock variable

    int lk5; // lock variable

    int lk6; // lock variable

    int lk7; // lock variable

    int lk8; // lock variable

    int lk9; // lock variable

    int lk10; // lock variable

    int lk11; // lock variable

    int lk12; // lock variable


    int cond;
    int p;
    int flag = 1;

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

        lk6 = 0; // initially lock is open

        lk7 = 0; // initially lock is open

        lk8 = 0; // initially lock is open

        lk9 = 0; // initially lock is open

        lk10 = 0; // initially lock is open

        lk11 = 0; // initially lock is open

        lk12 = 0; // initially lock is open


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

        if (p > 5) {
            lk6 = 1; // acquire lock
        } else {}

        if (p > 6) {
            lk7 = 1; // acquire lock
        } else {}

        if (p > 7) {
            lk8 = 1; // acquire lock
        } else {}

        if (p > 8) {
            lk9 = 1; // acquire lock
        } else {}

        if (p > 9) {
            lk10 = 1; // acquire lock
        } else {}

        if (p > 10) {
            lk11 = 1; // acquire lock
        } else {}

        if (p > 11) {
            lk12 = 1; // acquire lock
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

        if (p > 5) {
            flag = lk6;
            lk6 = 0;
        } else {}

        if (p > 6) {
            flag = lk7;
            lk7 = 0;
        } else {}

        if (p > 7) {
            flag = lk8;
            lk8 = 0;
        } else {}

        if (p > 8) {
            flag = lk9;
            lk9 = 0;
        } else {}

        if (p > 9) {
            flag = lk10;
            lk10 = 0;
        } else {}

        if (p > 10) {
            flag = lk11;
            lk11 = 0;
        } else {}

        if (p > 11) {
            flag = lk12;
            lk12 = 0;
        } else {}

    }
  out:
    return 0;
  ERROR:
    return 0;  
}

