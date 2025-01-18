// Variable x is not tainted, and the assertion agrees 
int main() {
    int x = 5;
    __VERIFIER_assert_taint(x,0);
}
