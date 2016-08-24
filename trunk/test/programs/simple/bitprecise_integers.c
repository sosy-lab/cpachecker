// some global variables for each type
char c;
unsigned char uc;
short s;
unsigned short us;
int i;
unsigned int ui;
long l;
unsigned long ul;
long long ll;
unsigned long long ull;


void assert(int expr) {
    if (expr == 0) { ERROR: goto ERROR; }
}


void overflow() {
    // all values are -2^n - 2
    c = -130;
    uc = -258;
    s = -32770;
    us = -65538;
    i = -2147483650LL;
    ui = -4294967298LL;
    ll = -9223372036854775810LL;
    ull = -18446744073709551618LL;

    assert(126 == c);
    assert(32766 == s);
    assert(2147483646 == i);
    assert(9223372036854775806LL == ll);
    assert(254 == uc);
    assert(65534 == us);
    assert(4294967294LL == ui);
    assert(18446744073709551614LL == ull);
}

void minusOne() {
    // all values are -1
    c = -1;
    uc = -1;
    s = -1;
    us = -1;
    i = -1;
    ui = -1;
    ll = -1;
    ull = -1;

    assert(-1 == c);
    assert(-1 == s);
    assert(-1 == i);
    assert(-1 == ll);
    assert(255 == uc);
    assert(65535 == us);
    assert(4294967295LL == ui);
    assert(18446744073709551615LL == ull);
    
    assert(0 > c);
    assert(0 > s);
    assert(0 > i);
    assert(0 > ll);
    assert(0 < uc);
    assert(0 < us);
    assert(0 < ui);
    assert(0 < ull);
}

void shiftOne() {
    // all values are -1
    c = -1;
    uc = -1;
    s = -1;
    us = -1;
    i = -1;
    ui = -1;
    ll = -1;
    ull = -1;
    
    // all values are -1
    c = c>>1;
    uc = uc>>1;
    s = s>>1;
    us = us>>1;
    i = i>>1;
    ui = ui>>1;
    ll = ll>>1;
    ull = ull>>1;

    assert(-1 == c);
    assert(-1 == s);
    assert(-1 == i);
    assert(-1 == ll);
    assert(127 == uc);
    assert(32767 == us);
    assert(2147483647 == ui);
    assert(9223372036854775807LL == ull);
}

void one() {
    // all values are 1
    c = 1;
    uc = 1;
    s = 1;
    us = 1;
    i = 1;
    ui = 1;
    ll = 1;
    ull = 1;

    assert(1 == c);
    assert(1 == s);
    assert(1 == i);
    assert(1 == ll);
    assert(1 == uc);
    assert(1 == us);
    assert(1 == ui);
    assert(1 == ull);
}

void shiftSize() {
    // all values are 0
    c = 1<<8;
    uc = 1<<8;
    s = 1<<16;
    us = 1<<16;
    i = 1<<32;
    ui = 1<<32;
    ll = 1<<64;
    ull = 1<<64;

    assert(0 == c);
    assert(0 == s);
    assert(0 == i);
    assert(0 == ll);
    assert(0 == uc);
    assert(0 == us);
    assert(0 == ui);
    assert(0 == ull);
}

int main() {
        
     overflow();
     minusOne();
     shiftOne();
     one();
     shiftSize();

    // 64bit only
    if (sizeof(long) == 8) {
        printf("64bit\n");

        ul = -2;
        ull = -1;
        assert(0 < ul);
        assert(ul < ull);
    }
    return (0);
}
