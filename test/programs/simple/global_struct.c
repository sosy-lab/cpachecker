#include <stdlib.h>
#include <string.h>
#include <stdio.h>

struct ts {
    int x;
    int y;
};

struct ts ats;

void second() {
    //ats.x = 3;  <--- BEACHTE
    if (ats.x == 1) {
        goto error;
        error: printf("error!\n");
    }
}

int main() {
    ats.x = 3;
    second();
    return 0;
}
