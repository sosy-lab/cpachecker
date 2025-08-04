extern int printf ( const char * format, ... );
extern int __VERIFIER_nondet_int();

int main(void) {
    int *myPointerA = ((void*) 0);
    int *myPointerB = ((void*) 0);

    if(__VERIFIER_nondet_int())
    {
        int myNumberA = 7;
        myPointerA = &myNumberA;
        // scope of myNumber ends here
    }

    int myNumberB = 3;
    myPointerB = &myNumberB;

    int sumOfMyNumbers = *myPointerA + *myPointerB; // myPointerA is out of scope
    printf("%d", sumOfMyNumbers);

    return 0;
}
