extern int __VERIFIER_nondet_int();
int main()
{
    int p1 = __VERIFIER_nondet_int();  // condition variable
    int lk1; // lock variable

    int p2 = __VERIFIER_nondet_int();  // condition variable
    int lk2; // lock variable

    int p3 = __VERIFIER_nondet_int();  // condition variable
    int lk3; // lock variable

    int p4 = __VERIFIER_nondet_int();  // condition variable
    int lk4; // lock variable

    int p5 = __VERIFIER_nondet_int();  // condition variable
    int lk5; // lock variable

    int flag = 1;
    int cond;

    while(1) {
        cond = __VERIFIER_nondet_int();
        if (cond == 0) {
            goto out;
        } else {}
        lk1 = 0; // initially lock is open

        lk2 = 0; // initially lock is open

        lk3 = 0; // initially lock is open

        lk4 = 0; // initially lock is open

        lk5 = 0; // initially lock is open


    // lock phase
        if (p1 > 0) {
            lk1 = 1; // acquire lock
        } else {}

        if (p2 > 0) {
            lk2 = 1; // acquire lock
        } else {}

        if (p3 > 0) {
            lk3 = 1; // acquire lock
        } else {}

        if (p4 > 0) {
            lk4 = 1; // acquire lock
        } else {}

        if (p5 > 0) {
            lk5 = 1; // acquire lock
        } else {}


    // unlock phase
        if (p1 > 0) {
	    flag = lk1;
            lk1 = 0;
        } else {}

        if (p2 > 0) {
	    flag = lk2;
            lk2 = 0;
        } else {}

        if (p3 > 0) {
	    flag = lk3;
            lk3 = 0;
        } else {}

        if (p4 > 0) {
	    flag = lk4;
            lk4 = 0;
        } else {}

        if (p5 > 0) {
	    flag = lk5;
            lk5 = 0;
        } else {}

    }
  out:
    return 0;
  ERROR:
    return 0;  
}

