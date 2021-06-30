extern void __VERIFIER_mark_tainted(void const *);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void)  {
    int arr[] = {100, 200, 300};
    int *ptr;
    ptr = arr;
    
    __VERIFIER_assert_untainted(arr);
    __VERIFIER_assert_untainted(ptr);
    __VERIFIER_mark_tainted(arr);
    __VERIFIER_assert_tainted(arr);
    __VERIFIER_assert_tainted(ptr);

	return 0;
}