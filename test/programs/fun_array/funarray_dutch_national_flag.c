extern int __VERIFIER_nondet_int(void);
extern void __VERIFIER_error(void);

int main(void) {

    int size = 20;
    int array[size];

    for (int i = 0; i < size; i++) {
        int x = __VERIFIER_nondet_int();
        if (x < 0 || x > 2) {
            return 0;
        }
        array[i] = x;
    }

    int right_border_red = 0;
    int right_border_white = 0;
    int left_border_blue = size - 1;


    while (right_border_white <= left_border_blue) {
        if (array[right_border_white] == 0) {
            array[right_border_white] = array[right_border_red];
            array[right_border_red] = 0;
            right_border_red++;
            right_border_white++;
        } else if (array[right_border_white] == 1) {
            right_border_white++;
        } else {
            int temp = array[left_border_blue];
            array[left_border_blue] = array[right_border_white];
            array[right_border_white] = temp;
            left_border_blue--;
        }
    }

    int current_segment = 0;
    for (int i = 0; i < size; i++) {
        if (array[i] < current_segment) {
            __VERIFIER_error();
        } else if (array[i] > current_segment) {
            current_segment++;
        }
    }

    return 0;
}