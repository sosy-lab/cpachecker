void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
    int a[3] = {0, 1, 2};
    for (int i=0; i<3; i++) {
        a[i] = 0;
    }
    assert(a[2] == 2);
}
