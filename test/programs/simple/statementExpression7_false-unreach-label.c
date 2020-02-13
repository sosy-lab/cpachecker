// break inside while loop condition
int main() {
    while (1) {
        int a = 5;
        while(({break; a++;})) { }
        
        if(a == 5) {
            ERROR: // reachable
                return 0;
        }
    }
    return 1;
}