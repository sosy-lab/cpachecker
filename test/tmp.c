/*
 *  * Date: 2012-08-10
 *   * Author: leike@informatik.uni-freiburg.de
 *    *
 *     * This program has the following 2-nested ranking function:
 *      * f_0(x, y) = y + 1
 *       * f_1(x, y) = x
 *        */

typedef enum {false, true} bool;

extern int __VERIFIER_nondet_int(void);

int main()
{
	    int x, y;
	            x = __VERIFIER_nondet_int();
		            y = __VERIFIER_nondet_int();
		if  (x >= 0) {
					                    x = x + y;
							                    y = y - 1;
									            }
				            return 0;
}
