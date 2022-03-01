int main() {

    int x = 0;
    int y = 4;

    if (y < 5) {
        if (y == 4) {
            x++;
            x++;
        }
        x++;
        y++;
        y++;
        y++;
    }

    if (x == 3) {
        ERROR: return 1;
    }
    return 0;
}
