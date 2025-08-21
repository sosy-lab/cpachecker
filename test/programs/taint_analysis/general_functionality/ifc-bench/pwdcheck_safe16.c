#include <stdlib.h>

//---------------------------------------------------------------------------//
// verifier functions                                                        //
//---------------------------------------------------------------------------//
extern void __VERIFIER_assume(int);
#define assume(X) __VERIFIER_assume(X)

extern int  __VERIFIER_nondet_int();

#define nd __VERIFIER_nondet_int
#define nd_ch (char)__VERIFIER_nondet_int

extern void  ifc_set_secret(int, ...);
extern void  ifc_set_live(int, ...);
extern void  ifc_check_out(int, ...);

#define N 8

int main() {
    char pwd[N]; // secret.
    char input[N]; // public.
    int i, j;
    int bad;

    // initialize pwd.
    for(i = 0; i < N-1; i++) {
//        char pwd_ch = (nd_ch());
        char pwd_ch = __VERIFIER_nondet_char();
//        ifc_set_secret(1, pwd_ch);
        __VERIFIER_set_public(pwd_ch, 0);
        assume(pwd_ch > 32 && pwd_ch < 127); // ascii
        pwd[i] = pwd_ch;
//        if (nd()) { break; }
        if (__VERIFIER_nondet_int()) { break; }
    }
    pwd[i] = '\0';
//    ifc_set_secret(1, i);
    __VERIFIER_set_public(i, 0);


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

//    ifc_check_out(1, j);
    __VERIFIER_is_public(j, 1);
    return 0;
}
