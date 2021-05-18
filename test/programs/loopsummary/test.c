typedef enum {false, true} bool;

extern int __VERIFIER_nondet_int(void);

int main() {
    int x, y, z;
        x = 0;
    y = 0;
    z = __VERIFIER_nondet_int();
    if (z > 0) {
        if (z > 2) {
            if (z > 0) {
                x = x + 1;
                y = y - 1;
                z = z - 1;
		x = x + (z - 1);
            	y = y + (1 - z);
            	z = z + (1 - z);
            	if (z > 0) {
                	x = x + 1;
                	y = y - 1;
                	z = z - 1;
            	}
            }
        }
    } else {
    	if (z > 0) {
            x = x + 1;
            y = y - 1;
            z = z - 1;
        }
        if (z > 0) {
            x = x + 1;
            y = y - 1;
            z = z - 1;
        }

    }
}
