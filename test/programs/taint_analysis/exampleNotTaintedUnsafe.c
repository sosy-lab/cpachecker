// Variable is not tainted, but the assertion sais otherwise
int main() {
    int x = 5;
    __VERIFIER_assert_taint(x,1);
}
