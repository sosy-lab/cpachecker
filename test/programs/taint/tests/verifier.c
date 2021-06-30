extern void __VERIFIER_mark_tainted(void const *);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d;
    char string[] = "Hello World";
    void *p = *a;
    __VERIFIER_mark_tainted(p);
    __VERIFIER_mark_tainted(string);
	
    __VERIFIER_mark_tainted(a);
	__VERIFIER_mark_tainted(b);
	__VERIFIER_mark_untainted(c);
    __VERIFIER_mark_untainted(d);
	__VERIFIER_assert_tainted(a);
    __VERIFIER_mark_untainted(b);
    __VERIFIER_assert_untainted(b);
    __VERIFIER_assert_untainted(c);
    __VERIFIER_mark_tainted(d);
    __VERIFIER_assert_tainted(d);
    __VERIFIER_assert_tainted(p);
	return 0;
}
