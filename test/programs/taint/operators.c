extern void __VERIFIER_tainted(int);
extern void __VERIFIER_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d, e, f, g, h;

    __VERIFIER_tainted(h);
    a = b = c = d = e = f = g = h;
    
    a = 2;
    b = 10;
    c = a + b;
    d = a + d;
    e = d + b;
    f = a - c;
    g = d - d;
    h = e - a;

	__VERIFIER_assert_untainted(a);
    __VERIFIER_assert_untainted(b);
    __VERIFIER_assert_untainted(c);
    __VERIFIER_assert_tainted(d);
    __VERIFIER_assert_tainted(e);
    __VERIFIER_assert_untainted(f);
    __VERIFIER_assert_untainted(g);
    __VERIFIER_assert_tainted(h);

	return 0;
}
