/* This test checks the work of InterruptFilter
 */
int global;

int deIntr() {
  global = 1;
}

int ldv_main() {
    deIntr();
}

