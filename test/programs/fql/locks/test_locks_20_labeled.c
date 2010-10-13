int main()
{
    int p1;  // condition variable
    int lk1; // lock variable

    int p2;  // condition variable
    int lk2; // lock variable

    int p3;  // condition variable
    int lk3; // lock variable

    int p4;  // condition variable
    int lk4; // lock variable

    int p5;  // condition variable
    int lk5; // lock variable

    int p6;  // condition variable
    int lk6; // lock variable

    int p7;  // condition variable
    int lk7; // lock variable

    int p8;  // condition variable
    int lk8; // lock variable

    int p9;  // condition variable
    int lk9; // lock variable

    int p10;  // condition variable
    int lk10; // lock variable

    int p11;  // condition variable
    int lk11; // lock variable

    int p12;  // condition variable
    int lk12; // lock variable

    int p13;  // condition variable
    int lk13; // lock variable

    int p14;  // condition variable
    int lk14; // lock variable

    int p15;  // condition variable
    int lk15; // lock variable

    int p16;  // condition variable
    int lk16; // lock variable

    int p17;  // condition variable
    int lk17; // lock variable

    int p18;  // condition variable
    int lk18; // lock variable

    int p19;  // condition variable
    int lk19; // lock variable

    int p20;  // condition variable
    int lk20; // lock variable


    int __BLAST_NONDET;


    int cond;

    while(1) {
        cond = __BLAST_NONDET;
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

        lk13 = 0; // initially lock is open

        lk14 = 0; // initially lock is open

        lk15 = 0; // initially lock is open

	lk16 = 0; // initially lock is open
	lk17 = 0; // initially lock is open
	lk18 = 0; // initially lock is open
	lk19 = 0; // initially lock is open
	lk20 = 0; // initially lock is open


	p1 = __BLAST_NONDET;
	p2 = __BLAST_NONDET;
	p3 = __BLAST_NONDET;
	p4 = __BLAST_NONDET;
	p5 = __BLAST_NONDET;
	p6 = __BLAST_NONDET;
	p7 = __BLAST_NONDET;
	p8 = __BLAST_NONDET;
	p9 = __BLAST_NONDET;
	p10 = __BLAST_NONDET;
	p11 = __BLAST_NONDET;
	p12 = __BLAST_NONDET;
	p13 = __BLAST_NONDET;
	p14 = __BLAST_NONDET;
	p15 = __BLAST_NONDET;
	p16 = __BLAST_NONDET;
	p17 = __BLAST_NONDET;
	p18 = __BLAST_NONDET;
	p19 = __BLAST_NONDET;
	p20 = __BLAST_NONDET;


    // lock phase
        if (p1 != 0) {
L: ;
            lk1 = 1; // acquire lock
        } else {}

        if (p2 != 0) {
            lk2 = 1; // acquire lock
        } else {}

        if (p3 != 0) {
            lk3 = 1; // acquire lock
        } else {}

        if (p4 != 0) {
            lk4 = 1; // acquire lock
        } else {}

        if (p5 != 0) {
            lk5 = 1; // acquire lock
        } else {}

        if (p6 != 0) {
            lk6 = 1; // acquire lock
        } else {}

        if (p7 != 0) {
            lk7 = 1; // acquire lock
        } else {}

        if (p8 != 0) {
            lk8 = 1; // acquire lock
        } else {}

        if (p9 != 0) {
            lk9 = 1; // acquire lock
        } else {}

        if (p10 != 0) {
            lk10 = 1; // acquire lock
        } else {}

        if (p11 != 0) {
            lk11 = 1; // acquire lock
        } else {}

        if (p12 != 0) {
            lk12 = 1; // acquire lock
        } else {}

        if (p13 != 0) {
            lk13 = 1; // acquire lock
        } else {}

        if (p14 != 0) {
            lk14 = 1; // acquire lock
        } else {}

        if (p15 != 0) {
            lk15 = 1; // acquire lock
        } else {}

        if (p16 != 0) {
            lk16 = 1; // acquire lock
        } else {}

        if (p17 != 0) {
            lk17 = 1; // acquire lock
        } else {}

        if (p18 != 0) {
            lk18 = 1; // acquire lock
        } else {}

        if (p19 != 0) {
            lk19 = 1; // acquire lock
        } else {}

        if (p20 != 0) {
            lk20 = 1; // acquire lock
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

        if (p3 != 0) {
            if (lk3 != 1) goto ERROR; // assertion failure
            lk3 = 0;
        } else {}

        if (p4 != 0) {
            if (lk4 != 1) goto ERROR; // assertion failure
            lk4 = 0;
        } else {}

        if (p5 != 0) {
            if (lk5 != 1) goto ERROR; // assertion failure
            lk5 = 0;
        } else {}

        if (p6 != 0) {
            if (lk6 != 1) goto ERROR; // assertion failure
            lk6 = 0;
        } else {}

        if (p7 != 0) {
            if (lk7 != 1) goto ERROR; // assertion failure
            lk7 = 0;
        } else {}

        if (p8 != 0) {
            if (lk8 != 1) goto ERROR; // assertion failure
            lk8 = 0;
        } else {}

        if (p9 != 0) {
            if (lk9 != 1) goto ERROR; // assertion failure
            lk9 = 0;
        } else {}

        if (p10 != 0) {
            if (lk10 != 1) goto ERROR; // assertion failure
            lk10 = 0;
        } else {}

        if (p11 != 0) {
            if (lk11 != 1) goto ERROR; // assertion failure
            lk11 = 0;
        } else {}

        if (p12 != 0) {
            if (lk12 != 1) goto ERROR; // assertion failure
            lk12 = 0;
        } else {}

        if (p13 != 0) {
            if (lk13 != 1) goto ERROR; // assertion failure
            lk13 = 0;
        } else {}

        if (p14 != 0) {
            if (lk14 != 1) goto ERROR; // assertion failure
            lk14 = 0;
        } else {}

        if (p15 != 0) {
            if (lk15 != 1) goto ERROR; // assertion failure
            lk15 = 0;
        } else {}

				if (p16 != 0) {
            if (lk16 != 1) goto ERROR; // assertion failure
            lk16 = 0;
        } else {}

				if (p17 != 0) {
            if (lk17 != 1) goto ERROR; // assertion failure
            lk17 = 0;
        } else {}

				if (p18 != 0) {
            if (lk18 != 1) goto ERROR; // assertion failure
            lk18 = 0;
        } else {}

				if (p19 != 0) {
            if (lk19 != 1) goto ERROR; // assertion failure
            lk19 = 0;
        } else {}

				if (p20 != 0) {
            if (lk20 != 1) goto ERROR; // assertion failure
            lk20 = 0;
        } else {}

    }
  out:
    return 0;
  ERROR:
    return 0;  
}

