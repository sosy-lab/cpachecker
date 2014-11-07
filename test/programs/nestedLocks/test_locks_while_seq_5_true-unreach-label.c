extern int __VERIFIER_nondet_int();
int main()
{
    int lk1 = 0; // lock variable, initially lock is open

    int lk2  = 0; // lock variable, initially lock is open

    int lk3 = 0; // lock variable, initially lock is open

    int lk4 = 0; // lock variable, initially lock is open

    int lk5 = 0; // lock variable, initially lock is open


    int cond = __VERIFIER_nondet_int();

    while(cond) {
        
    // lock phase
        if (cond == 1) {
            lk1 = 1; // acquire lock
        } else {
	    cond = 2;
	}

    // unlock phase
        if (cond == 1) {
            if (lk1 != 1) goto ERROR; // assertion failure
            lk1 = 0;
        } else {}

    	cond = __VERIFIER_nondet_int();

    }

    cond = __VERIFIER_nondet_int();

    while(cond) {
        
    // lock phase
        if (cond == 1) {
            lk2 = 1; // acquire lock
        } else {
	    cond = 2;
	}

    // unlock phase
        if (cond == 1) {
            if (lk2 != 1) goto ERROR; // assertion failure
            lk2 = 0;
        } else {}

    	cond = __VERIFIER_nondet_int();

    }

    cond = __VERIFIER_nondet_int();

    while(cond) {
        
    // lock phase
        if (cond == 1) {
            lk3 = 1; // acquire lock
        } else {
	    cond = 2;
	}

    // unlock phase
        if (cond == 1) {
            if (lk3 != 1) goto ERROR; // assertion failure
            lk3 = 0;
        } else {}

    	cond = __VERIFIER_nondet_int();

    }

    cond = __VERIFIER_nondet_int();

    while(cond) {
        
    // lock phase
        if (cond == 1) {
            lk4 = 1; // acquire lock
        } else {
	    cond = 2;
	}

    // unlock phase
        if (cond == 1) {
            if (lk4 != 1) goto ERROR; // assertion failure
            lk4 = 0;
        } else {}

    	cond = __VERIFIER_nondet_int();

    }

    cond = __VERIFIER_nondet_int();

    while(cond) {
        
    // lock phase
        if (cond == 1) {
            lk5 = 1; // acquire lock
        } else {
	    cond = 2;
	}

    // unlock phase
        if (cond == 1) {
            if (lk5 != 1) goto ERROR; // assertion failure
            lk5 = 0;
        } else {}

    	cond = __VERIFIER_nondet_int();

    }

    return 0;
  ERROR:
    return 0;  
}

