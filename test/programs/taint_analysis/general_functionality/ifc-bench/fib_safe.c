extern int nd(void);
extern void __VERIFIER_error(void) __attribute__((noreturn));
#define assert(X) if(!(X)){__VERIFIER_error();}
extern void __VERIFIER_assume(int);
#define assume(X) __VERIFIER_assume(X)
extern void __VERIFIER_keepalive(int);
extern void  ifc_set_secret(int, ...);
extern void  ifc_set_low(int, ...);
extern void  ifc_check_out(int, ...);

int main()
{
//    int n = nd();
    int n = __VERIFIER_nondet_int();
//    ifc_set_low(1, n);
    __VERIFIER_set_public(n, 1);
    assume(n>2);

    int a = 1;
    int b = 1;

    int i = 2;

    for (; i <= n; i++) {
      int c = a+b;
      a = b;
      b = c;
    }

//    ifc_check_out(1, b);
    __VERIFIER_is_public(b, 1);
    return 0;
}
