int main() {
    int x = 0;
    while (x != 100) {
        x++;
    }
    if (x != 100)
        goto ERROR;
    return 0;
ERROR: return 1;
}
