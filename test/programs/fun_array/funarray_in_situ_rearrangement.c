#include <stdbool.h>

extern int __VERIFIER_nondet_int(void);
extern void __VERIFIER_error(void);

int main(void) {

    int size = 100;
    int array[size];

    for (int i = 0; i < size; i++) {
      array[i] = __VERIFIER_nondet_int();
    }

    int border_positive = 0;
    int border_negative = size;

    while (border_positive<border_negative) {
        if (array[border_positive] >= 0) {
            border_positive = border_positive + 1;
        } else {
            border_negative = border_negative - 1;
            int x = array[border_positive];
            array[border_positive] = array[border_negative];
            array[border_negative] = x;
        }
    }

    bool in_positive_segment = true;

    for (int i = 0; i < size; i++) {
        if (in_positive_segment) {
            if (array[i] < 0) {
                in_positive_segment = false;
            }
        } else {
            if (array[i] >= 0) {
                __VERIFIER_error();
            }
        }
    }

    return 0;
}