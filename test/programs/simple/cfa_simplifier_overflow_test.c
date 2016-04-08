void main() {
    
    int a;
    a = -2147483648; // no change
    a = -2147483649; // overflow to 2147483647, because the number is an INT
    a = -2147483648LL; // no change
    a = -2147483649LL; // no change
    
    long long int b;
    b = -2147483648; // no change
    b = -2147483649; // overflow to 2147483647, because the number is an INT
    b = -2147483648LL; // no change
    b = -2147483649LL; // no change
    
    char c;
    c = -2147483648; // no change
    c = -2147483649; // overflow to 2147483647, because the number is an INT
    c = -2147483648LL; // no change
    c = -2147483649LL; // no change
}
