/*@
ensures x == 0;
behavior one:
assumes \true;
ensures !(x > 0);
behavior two:
assumes \true;
ensures !(x < 0);
*/
int main() {
    int x,y;
    x = 10;
    y = 20;
    while(x > 0) {
        y = y - 2;
        //@ assert y % 2 == 0;
        x--;
        //@ assert y == x * 2;
    }
    if(y) {
        ERROR: return 1;
    }
    return 0;
}
