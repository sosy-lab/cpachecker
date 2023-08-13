int main() {
    
    int *p;
    *p = 1;
    for (int i = 1; i <= 10; i++) {
        *p *= 2;
    }

    if (*p == 1024) { ERROR : ;}
    
}
