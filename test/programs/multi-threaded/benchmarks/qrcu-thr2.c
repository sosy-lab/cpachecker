// #include <assert.h> //CP: gcc preprocessor inlines the assert
#include <assert.h>
// #include <pthread.h>

int idx = 0; // bit idx = 0; controls which of the two elements ctr1 or ctr2 will be used by readers
int ctr1 = 1;
int ctr2 = 0; // byte ctr[2];
int readerprogress1 = 0;
int readerprogress2 = 0; // byte readerprogress[N_QRCU_READERS];
int mutex = 0; // bit mutex = 0; updates are done in critical section, only one writer at a time
int __BLAST_NONDET;

void main() {
  int myidx;

  /* rcu_read_lock */
  while (1) {
    myidx = idx;
    if (__BLAST_NONDET) {
      while(myidx > 0);
      while(ctr1 <= 0);
      ctr1++;

      break;
    } else {
      if (__BLAST_NONDET) {
        while(myidx <= 0);
        while(ctr2 <= 0);
        ctr2++;

        break;
      } else {}
    }
  }

  readerprogress2 = 1; /*** readerprogress[me] = 1; ***/
  readerprogress2 = 2; /*** readerprogress[me] = 2 ***/

  /* rcu_read_unlock */

  if (myidx <= 0) { ctr1--; } // use ctr1
  else { ctr2--; } // use ctr2

}
