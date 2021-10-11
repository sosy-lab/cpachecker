int main() {

    int x = 5;
    if (x != 5) {
        while(x != 0) {
            LOOP: x--;
        }
        goto ERROR;
    }
    goto LOOP;
    ERROR: return 1;
}
