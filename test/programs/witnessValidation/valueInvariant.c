int main(void) {
    int a = 5;
    int b = 10; 
    if (a < 0) {
        b = 9;   
    } else {
        a = 20;
    }
    if (a + b < 15) {
        ERROR: return 1;
    }
    return 0;
}
