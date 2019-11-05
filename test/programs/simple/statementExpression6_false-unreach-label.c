// macro variable shadowing
#define maxint(a,b) ({int _a = (a), _b = (b); _a > _b ? _a : _b; })

int main() {
    int _a = 1, _b = 2, c;
    c = maxint (_a, _b);
    
    // maxint does not necessarily return the max of _a and _b (but it can)
    if (c != _a && c != _b) {
        ERROR: // reachable
            return 1;
    }

    return 0;
}