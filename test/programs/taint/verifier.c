extern void __VERIFIER_tainted(int);
extern void __VERIFIER_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d;
	__VERIFIER_tainted(a);
	__VERIFIER_tainted(b);
	__VERIFIER_untainted(c);
    __VERIFIER_untainted(d);
	__VERIFIER_assert_tainted(a);
    __VERIFIER_untainted(b);
    __VERIFIER_assert_untainted(b);
    __VERIFIER_assert_untainted(c);
    __VERIFIER_tainted(d);
    __VERIFIER_assert_tainted(d);
	return 0;
}
