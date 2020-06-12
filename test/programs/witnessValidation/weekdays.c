enum Weekday {MON=1, TUE, WED, THU, FRI, SAT, SUN=0};

int main(void) {
    enum Weekday x = MON;
    while (x <= SAT) {
        x = x + 1;
    }
    if(x % 7 != SUN) {
        ERROR: return 1;
    }
    return 0;
}
