
int main() {
    int a = 10 + 10;
    int b = 1+2+3+4+5;
    
    if (b-2 != 6+7) {
        goto ERROR;
    }
    
    if (10*2!=a) {
        goto ERROR;
    }
    
    return (0);
    ERROR: 
    return (-1);
}
