#include<assert.h>

int main() {

  // Demonstrates effect of the option cpa.stator.policy.runAcceleratedValueDetermination.
    int a, b, c, d, e;
    int sum = 0;
    int i;
    a = 1;
    b = 1;
    c = 1;
    d = 1;
    e = 1;
    for (i=0; i<=1000; i++) {
        a = 100;
        b = 500;
        c = 600;
        d = 800;
        e = 900;
        sum += 1;
    }
    a = 1;
    b = 1;
    c = 1;
    d = 1;
    e = 1;
    for (i=0; i<=1000; i++) {
        a = 100;
        b = 500;
        c = 600;
        d = 800;
        e = 900;
        sum += 1;
    }
    a = 1;
    b = 1;
    c = 1;
    d = 1;
    e = 1;
    for (i=0; i<=1000; i++) {
        a = 100;
        b = 500;
        c = 600;
        d = 800;
        e = 900;
        sum += 1;
    }
    a = 1;
    b = 1;
    c = 1;
    d = 1;
    e = 1;
    for (i=0; i<=1000; i++) {
        a = 100;
        b = 500;
        c = 600;
        d = 800;
        e = 900;
        sum += 1;
    }
    assert(i == 1001);
}
