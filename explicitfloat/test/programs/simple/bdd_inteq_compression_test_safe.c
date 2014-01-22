
int main() {
    int a;
    if (a!=5) 
    if (a!=6)
    if ((a!=7)==7) { // never fullfilled, because 7 is not a boolean value
        ERROR:goto ERROR;
    }
    
    return (0);
}
