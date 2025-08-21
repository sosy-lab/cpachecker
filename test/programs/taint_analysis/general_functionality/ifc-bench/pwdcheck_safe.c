#include <stdlib.h>

//---------------------------------------------------------------------------//
// verifier functions                                                        //
//---------------------------------------------------------------------------//
extern void __VERIFIER_assume(int);
#define assume(X) __VERIFIER_assume(X)

extern int  __VERIFIER_nondet_int();

#define nd __VERIFIER_nondet_int
#define nd_ch (char)__VERIFIER_nondet_int

//---------------------------------------------------------------------------//
// IFC functions:
// set_secret defines a variable as HIGH
// check_out is for SC checks
//---------------------------------------------------------------------------//
extern void  ifc_set_secret(char*);
extern void  ifc_check_taint(int*);
extern void  ifc_set_notaint(int*);
extern void  ifc_check_out(int*);

#define N 8

int main() {

// This is also an option for modeling arbitrary length
//    int N=0;
//    ifc_set_notaint(&N);
//    N = nd();

    char pwd[N]; // secret.
    char input[N]; // public.
    int i, j;
    int bad=0;

    // initialize pwd.
    for(i = 0; i < N-1; i++) {
//        char pwd_ch = (nd_ch());
        char pwd_ch = __VERIFIER_nondet_char();
//        ifc_set_secret(&pwd_ch);
        __VERIFIER_set_public(pwd_ch, 0);
        assume(pwd_ch > 32 && pwd_ch < 127); // ascii
        pwd[i] = pwd_ch;
//        if (nd()) { break; }
        if (__VERIFIER_nondet_int()) { break; }
    }
    pwd[i] = '\0';

    // initialize input
    for(i = 0; i < N-1; i++) {
//        input[i] = (nd_ch());
        input[i] = __VERIFIER_nondet_char();
        assume((int)input[i] > 32 && (int)input[i] < 127); // ascii
//        if (nd()) { break; }
        if (__VERIFIER_nondet_int()) { break; }
    }
    input[i] = '\0';


    // now compare the two.
    bad = 0;
    for(j = 0; j < N && input[j]; j++) {
        if (!bad) {
            if (pwd[j] == '\0') { bad = 1; }
            else if (pwd[j] != input[j]) { bad = 1; }
        }
    }

//    ifc_check_taint(&j);
//    ifc_check_out(&j);
    __VERIFIER_is_public(j, 1);
}
