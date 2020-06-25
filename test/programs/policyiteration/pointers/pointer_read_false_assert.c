void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
    int a, undefined;
    int *pointer;
    a = 1;
    if (undefined) {
        pointer = &a;
    }
    *pointer = 42;
    assert(a == 42);
    return 0;
}
