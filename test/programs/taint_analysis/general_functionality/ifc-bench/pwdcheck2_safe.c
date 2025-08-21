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
extern void  ifc_set_notaint(char*);
extern void  ifc_set_notaint2(int*);
extern void  ifc_check_taint(int*);
extern void  ifc_check_out(int*);

extern int f();
extern int g();
extern int h();

//#define N 32

int main() {
//    int N = nd();
    int N = __VERIFIER_nondet_int();
    assume (N > 11);
//    ifc_set_notaint2(&N);
    __VERIFIER_set_public(&N, 1);

    char pwd[N]; // secret.
    char input[N]; // public.
    char in_nd[N];
    int nd_arr[N];
    int i=0, j=0;
    int bad=0;
    int h = __VERIFIER_nondet_int();
    char x = 0;
    int y = 0;

//    ifc_set_notaint2(&h);
    __VERIFIER_set_public(&h, 1);

    for(i = 0; i < N-1; i++) {
//        h = nd();
        h = __VERIFIER_nondet_int();
        nd_arr[i] = h;
    }

//    ifc_set_notaint(&x);
    __VERIFIER_set_public(&x, 1);
    for(i = 0; i < N-1; i++) {
//        x = nd_ch();
        x = __VERIFIER_nondet_char();
        in_nd[i] = x;
    }


    // initialize pwd.
    for(i = 0; i < N-1; i++) {
//        char pwd_ch = nd_ch();
        char pwd_ch = __VERIFIER_nondet_char();
//        ifc_set_secret(&pwd_ch);
        __VERIFIER_set_public(&pwd_ch, 0);
        assume(pwd_ch > 32 && pwd_ch < 127); // ascii
        pwd[i] = pwd_ch;
//        h = nd();
        h = __VERIFIER_nondet_char();
        if (h) { break; }
    }
    pwd[i] = '\0';

    // initialize input
    for(i = 0; i < N-1; i++) {
        input[i] = in_nd[i];
        assume((int)input[i] > 32 && (int)input[i] < 127); // ascii
        if (nd_arr[i]) { break; }
    }
    input[i] = '\0';


    // now compare the two.
    // Note the use of the assume. The reason is to prevent
    // LLVM/SeaHorn from simplifying this problem and figuring
    // out everything automatically.
    bad = 0;
    for(j = 0; j < N && !bad; j++) {
        if (pwd[j] == '\0') { y = f(); assume(y==1); bad = y; }
        else if (pwd[j] != input[j]) { y = g(); assume(y==1); bad = y;}
        if (j > 10 ) { y = f(); assume(y==1); assume(bad == y);}
    }

    //if (!bad) bad = 1;

//    ifc_check_taint(&bad);
//    ifc_check_out(&bad);
    __VERIFIER_is_public(&bad, 0);
}
