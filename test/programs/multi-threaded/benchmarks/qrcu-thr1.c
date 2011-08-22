#include <assert.h>

int idx = 0; // bit idx = 0; controls which of the two elements ctr1 or ctr2 will be used by readers
int ctr1 = 1;
int ctr2 = 0; // byte ctr[2];
int readerprogress1 = 0;
int readerprogress2 = 0; // byte readerprogress[N_QRCU_READERS];
int mutex = 0; // bit mutex = 0; updates are done in critical section, only one writer at a time
int __BLAST_NONDET;


/* sums the pair of counters forcing weak memory ordering */
#define sum_unordered                           \
  if (__BLAST_NONDET) {                         \
    sum = ctr1;                                 \
    sum = sum + ctr2;                           \
  } else {                                      \
    sum = ctr2;                                 \
    sum = sum + ctr1;                           \
  }

void main() {
  int i;
  int readerstart1;
  int readerstart2;
  int sum;


  /* Snapshot reader state. */

  readerstart1 = readerprogress1;
  readerstart2 = readerprogress2;


  sum_unordered;
  if (sum <= 1) { sum_unordered; }
  if (sum > 1) {
    /* acquire mutex */
    while (mutex != 0);
    mutex = 1;


    if (idx <= 0) { ctr2++; idx = 1; ctr1--; }
    else { ctr1++; idx = 0; ctr2--; }
    if (idx <= 0) { while (ctr2 > 0); }
    else { while (ctr1 > 0); }

    /* relase mutex */
    mutex = 0;
  }

  /* Verify reader progress. */

  if (__BLAST_NONDET) {
    while (readerstart1 != 1);
    while(readerprogress1 != 1);
    assert(0);
  } else {
    if (__BLAST_NONDET) {
      while(readerstart2 != 1);
      while(readerprogress2 != 1);
      assert(0);
    } else { }
  }


}



