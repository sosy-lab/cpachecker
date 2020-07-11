/*@
    ensures x == \old(x);
    ensures y >= 0;
    behavior positive:
    assumes x >= 0;
    ensures y == x;
    behavior negative:
    assumes x < 0;
    ensures y == -x;
    complete behaviors;
    disjoint behaviors;
*/
int abs(int x) {
    int y;    
    if (x < 0) {
        y = -x;
    } else {
        y = x;
    }    
    return y;
}

int main(void) {
    abs(10);
    return 0;
}
