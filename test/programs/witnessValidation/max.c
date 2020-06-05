int main(void) {
    int x = 10, y = 20, z = max(x, y);
    if(z < x || z < y) {
        ERROR: return 1;
    }
    return 0;
}

int max(int a, int b) {
    if(a <= b) {
        return b;
    }
    return a;
}
