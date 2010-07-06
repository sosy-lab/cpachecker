int get_exit_nondet()
{
    int retval;
    return (retval);
}

int main()
{
    int p1;  // condition variable
    int lk1; // lock variable

    int p2;  // condition variable
    int lk2; // lock variable


    int cond;

    while(1) {
        cond = get_exit_nondet();
        if (cond == 0) {
            goto out;
        } else {}
        lk1 = 0; // initially lock is open

        lk2 = 0; // initially lock is open


    // lock phase
        if (p1 != 0) {
            lk1 = 1; // acquire lock
        } else {}

        if (p2 != 0) {
            lk2 = 1; // acquire lock
        } else {}



    // unlock phase
        if (p1 != 0) {
            if (lk1 != 1) goto ERROR; // assertion failure
            lk1 = 0;
        } else {}

        if (p2 != 0) {
            if (lk2 != 1) goto ERROR; // assertion failure
            lk2 = 0;
        } else {}


    }
  out:
    return 0;
  ERROR:
    return 0;  
}

