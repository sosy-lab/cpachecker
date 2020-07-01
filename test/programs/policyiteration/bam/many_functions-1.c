void assert(int cond) { if (!cond) { ERROR: return; } }

const int BOUND = 10;

int test(int input);

int main() {
    int a = test(0);
    int b = test(0);
    assert(a + b > BOUND);
}

int test(int input) {
    for (int i=0; i<BOUND; i++) {
        input++;
    }
    return input;
}

