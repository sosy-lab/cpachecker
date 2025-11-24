extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
    const int length = __VERIFIER_nondet_int();
    const int split = __VERIFIER_nondet_int();

    if (split < 0 || split >= length) {
        return 0;
    }

    int array[length];

    for (int i = 0; i < split; ++i) {
        array[i] = 0;
    }

    for (int i = split; i < length; ++i) {
        array[i] = 1;
    }

    const int random_element_index = __VERIFIER_nondet_int();

    if (random_element_index < 0 || random_element_index >= length) {
        return 0;
    }

    if (random_element_index < split) {
        if (array[random_element_index] != 0) {
            __VERIFIER_error();
        }
    } else {
        if (array[random_element_index] != 1) {
            __VERIFIER_error();
        }
    }

    return 0;
}