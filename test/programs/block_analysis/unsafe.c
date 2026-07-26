int water = 1;     // 0..2
int methane = 0;   // 0/1
int pump = 0;      // 0/1

extern int __VERIFIER_nondet_int();

int t() {return __VERIFIER_nondet_int();}

int main(void) {
    for (int i = 0; i < 3; i++) {
        /* environment */

        if (pump && water > 0) water--;
        if (t() && water < 2) water++;
        if (t()) methane ^= 1;

        /* controller */
        if (pump && methane) pump = 0;
        if (!pump && water == 2 && !methane) pump = 1;


        /* safety */
        if (pump && water == 0) reach_error();
        
    
    }
}
