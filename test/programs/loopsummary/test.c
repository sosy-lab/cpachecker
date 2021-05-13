/*
 * Program used in the experimental evaluation of the following papers.
 * 2008ESOP - Chawdhary,Cook,Gulwani,Sagiv,Yang - Ranking Abstractions
 * 2010SAS - Alias,Darte,Feautrier,Gonnord, Multi-dimensional Rankings, Program
 *           Termination, and Complexity Bounds of Flowchart Programs
 *
 * Date: 2014
 * Author: Caterina Urban
 */

typedef enum {false, true} bool;

extern int __VERIFIER_nondet_int(void);

int main() {
    int x, y, z;
        x = 0;
    y = 0;
    z = __VERIFIER_nondet_int();
    if (z > 0) {
        if (z - 2 > 0) {
            if (z > 0) {
                x = x + 1;
                y = y - 1;
                z = z - 1;
            }
            x = x + (z - 1);
            y = y + (1 - z);
            z = z + (1 - z);
            if (z > 0) {
                x = x + 1;
                y = y - 1;
                z = z - 1;
            }
        }
        
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
