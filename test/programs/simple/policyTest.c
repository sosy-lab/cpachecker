int main() {
    int sum = 0;
    int k = 0;
    for (int i=0; i<10; i++) {
        sum++;
    }
    if (sum < 5) {
        goto ERROR;
    }
    return 0;
ERROR:
    return -1;
}
