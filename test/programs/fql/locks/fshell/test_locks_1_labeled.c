int input();

int main()
{
    int p1;  // condition variable
    int lk1; // lock variable

    int cond;

    while(1) {
        cond = input();
        if (cond == 0) {
            goto out;
        } else {}
        lk1 = 0; // initially lock is open


	p1 = input();


    // lock phase
        if (p1 != 0) {
L: ;
            lk1 = 1; // acquire lock
        } else {}




    // unlock phase
        if (p1 != 0) {
            if (lk1 != 1) goto ERROR; // assertion failure
            lk1 = 0;
        } else {}



    }
  out:
    return 0;
  ERROR:
    return 0;  
}

