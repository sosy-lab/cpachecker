int main() {

    int i = 0;
    for (i = 0; i < 6; i++) {

    }
    if (i != 6)
        goto ERROR;
    return 0;
    ERROR: return 1;
}
