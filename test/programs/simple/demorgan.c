int x=0;

int main() {
    int a, b;
    
    if (!(a&&b)) {
        x=1;
    }

    if (!a||!b) {
        x=2;
    }
    
    // 5x NOT is 1x NOT
    if (!(!(!(!(!(a&&b)))))) {
        x=3;
    }
    
    // 4x NOT is no NOT
    if (!(!(!(!(a&&b))))) {
        x=4;
    }
    
    return 0; 
}
