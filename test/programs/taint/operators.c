extern void __VERIFIER_mark_tainted(int);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d, e;
    __VERIFIER_mark_tainted(e);
    a = b = c = d = e;

    __VERIFIER_assert_tainted(a);
    __VERIFIER_assert_tainted(e);
    
    a = 2;
    b = 10;
    c = a + b;
    d = a + d;
    e = d + c;

    __VERIFIER_assert_untainted(a);
    __VERIFIER_assert_untainted(b);
    __VERIFIER_assert_untainted(c);
    __VERIFIER_assert_tainted(d);
    __VERIFIER_assert_tainted(e);

    e = d + b;
    __VERIFIER_assert_tainted(e);
    e = a - c;
    __VERIFIER_assert_untainted(e);
    e = d + b;
    __VERIFIER_assert_tainted(e);
    e = d - d;
    __VERIFIER_assert_untainted(e);
    e = d;
    __VERIFIER_assert_tainted(e);
    e = 5;
    __VERIFIER_assert_untainted(e);
    e = 5 + 5;
    __VERIFIER_assert_untainted(e);
    
    printf("%d", e);
    e = a + 5;
    __VERIFIER_assert_untainted(e);
    e = d + 5;
    __VERIFIER_assert_tainted(e);
    
    e = a - 5;
    __VERIFIER_assert_untainted(e);
    e = d - 5;
    __VERIFIER_assert_tainted(e);

    e = a / 5;
    __VERIFIER_assert_untainted(e);
    e = d / 5;
    __VERIFIER_assert_tainted(e);

    e = a * 5;
    __VERIFIER_assert_untainted(e);
    e = d * 5;
    __VERIFIER_assert_tainted(e);

    int x = 3;
    __VERIFIER_assert_untainted(x);

	return 0;
}
