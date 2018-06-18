void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
    int a = 10;
    int *p = &a;
    *p = 42;
    assert(a  == 42);
}
