/*
The program TriPerimetre (implemented in the function foo) 
has the exact same control structure of Tritype. The difference 
is that TriPerimetre returns a calculation on the input and not 
a constant value. The program returns the sum of the triangle 
sides if the input conrresponds to a valid triangle; and 
returns -1 otherwise. 

THe error is this program is in the second part of the condition 
in the condition condition "(trityp == 1 && (i+j) > k)", 
the correct instruction should be (trityp == 2 && (i+k) > j).
By taking the input {i=1, j=2, k=1}, the program returns 
the value 4, however, it should return the value -1 to 
indicate the fact that the input does not correspond to a 
valid triangle. 

SPDX-FileCopyrightText: Mohammed Bekkouche <http://www.i3s.unice.fr>
SPDX-License-Identifier: GPL-3.0-or-later
*/

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
} 

/* program for triangle perimeter 
 * returns i+j+k
 *      i+j+k if (i,j,k) are the sides of any triangle
 *      2*i + j or 2*i+k or 2*j+i if (i,j,k) are the sides of an isosceles triangle
 *      3*i if (i,j,k) are the sides of an equilateral triangle
 * returns -1 if (i,j,k) are not the sides of a triangle
 * 
 * ERROR : in assignment line 34
 * returns 9 for (2,3,2) while it should return 7
 */
void foo (int i, int j, int k) {//__CPROVER_assume((i == 2) && (j == 3) && (k ==2));
    int trityp = 0;
    int res;
    if (i == 0 || j == 0 || k == 0) {
        trityp = 4;	
        res = -1;
    }
    else {
        trityp = 0;
        if (i == j) {
            trityp = trityp + 1;
            res = 2*i;
        }
        if (i == k) {
            trityp = trityp + 2;
            res = 2*j;  // error in the assignment : should be res = 2*i
        }
        if (j == k) {
            trityp = trityp + 3;
            res = 2*j;
        }
        if (trityp == 0) {
            if ((i+j) <= k || ((j+k) <= i || (i+k) <= j)) {
                trityp = 4;
                res = -1;
            }
            else {
                trityp = 1;
                res = i+j+k;
            }			
        }
        else {
            if (trityp > 3) {
                res = res + i;
            }
            else {
                if (trityp == 1 && (i+j) > k) { // i==j
                    res=res+k; 
                }
                else {						
                    if (trityp == 2 && (i+k) > j) {    // i==k
                        res = res + j;						
                    }
                    else {
                        if (trityp == 3 && (j+k) > i) {   // j==k
                            res=res+i;
                        }
                        else {
                            res=-1;
                        }
                    }
                }
            }
        }
    }
    //assert(res == 7);
    
    __VERIFIER_assert(res== ((i==0 || j==0 || k==0)?-1:(  (i!=j && i!=k && j!=k)?(   ((i+j)<=k || (j+k)<=i || (i+k)<=j)?-1:(i+j+k)   ):( ((i==j && j==k) || (j==k && i==k))?(3*i):( (i==j && i!=k && j!=k && (i+j)>k)?(2*i+k):( (i!=j && j!=k && i==k && (i+k)>j)?(2*i+j):( ( ((i!=j && j==k && i!=k) || (i==j && j!=k && i==k)) && (j+k)>i)?(2*j+i):-1 ) ) ) ) )) );
    
}
int main() 
{ 
  
  foo( __VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    }
