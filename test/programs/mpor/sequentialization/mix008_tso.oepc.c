extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
extern _Bool __VERIFIER_nondet_bool(void);
extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }
void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();} }; return; }
extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

#include <assert.h>
#include <pthread.h>
#ifndef TRUE
#define TRUE (_Bool)1
#endif
#ifndef FALSE
#define FALSE (_Bool)0
#endif
#ifndef NULL
#define NULL ((void*)0)
#endif
#ifndef FENCE
#define FENCE(x) ((void)0)
#endif
#ifndef IEEE_FLOAT_EQUAL
#define IEEE_FLOAT_EQUAL(x,y) (x==y)
#endif
#ifndef IEEE_FLOAT_NOTEQUAL
#define IEEE_FLOAT_NOTEQUAL(x,y) (x!=y)
#endif



void * P0(void *arg);


void * P1(void *arg);


void * P2(void *arg);


void * P3(void *arg);


void fence();


void isync();


void lwfence();




int __unbuffered_cnt;


int __unbuffered_cnt = 0;


int __unbuffered_p2_EAX;


int __unbuffered_p2_EAX = 0;


int __unbuffered_p2_EBX;


int __unbuffered_p2_EBX = 0;


int __unbuffered_p3_EAX;


int __unbuffered_p3_EAX = 0;


int __unbuffered_p3_EBX;


int __unbuffered_p3_EBX = 0;


int a;


int a = 0;


_Bool main$tmp_guard0;


_Bool main$tmp_guard1;


int x;


int x = 0;


int y;


int y = 0;


_Bool y$flush_delayed;


int y$mem_tmp;


_Bool y$r_buff0_thd0;


_Bool y$r_buff0_thd1;


_Bool y$r_buff0_thd2;


_Bool y$r_buff0_thd3;


_Bool y$r_buff0_thd4;


_Bool y$r_buff1_thd0;


_Bool y$r_buff1_thd1;


_Bool y$r_buff1_thd2;


_Bool y$r_buff1_thd3;


_Bool y$r_buff1_thd4;


_Bool y$read_delayed;


int *y$read_delayed_var;


int y$w_buff0;


_Bool y$w_buff0_used;


int y$w_buff1;


_Bool y$w_buff1_used;


int z;


int z = 0;


_Bool z$flush_delayed;


int z$mem_tmp;


_Bool z$r_buff0_thd0;


_Bool z$r_buff0_thd1;


_Bool z$r_buff0_thd2;


_Bool z$r_buff0_thd3;


_Bool z$r_buff0_thd4;


_Bool z$r_buff1_thd0;


_Bool z$r_buff1_thd1;


_Bool z$r_buff1_thd2;


_Bool z$r_buff1_thd3;


_Bool z$r_buff1_thd4;


_Bool z$read_delayed;


int *z$read_delayed_var;


int z$w_buff0;


_Bool z$w_buff0_used;


int z$w_buff1;


_Bool z$w_buff1_used;


_Bool weak$$choice0;


_Bool weak$$choice2;



void * P0(void *arg)
{
  __VERIFIER_atomic_begin();
  a = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  x = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}



void * P1(void *arg)
{
  __VERIFIER_atomic_begin();
  x = 2;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  y = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  y = y$w_buff0_used && y$r_buff0_thd2 ? y$w_buff0 : (y$w_buff1_used && y$r_buff1_thd2 ? y$w_buff1 : y);
  y$w_buff0_used = y$w_buff0_used && y$r_buff0_thd2 ? FALSE : y$w_buff0_used;
  y$w_buff1_used = y$w_buff0_used && y$r_buff0_thd2 || y$w_buff1_used && y$r_buff1_thd2 ? FALSE : y$w_buff1_used;
  y$r_buff0_thd2 = y$w_buff0_used && y$r_buff0_thd2 ? FALSE : y$r_buff0_thd2;
  y$r_buff1_thd2 = y$w_buff0_used && y$r_buff0_thd2 || y$w_buff1_used && y$r_buff1_thd2 ? FALSE : y$r_buff1_thd2;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}



void * P2(void *arg)
{
  __VERIFIER_atomic_begin();
  y$w_buff1 = y$w_buff0;
  y$w_buff0 = 2;
  y$w_buff1_used = y$w_buff0_used;
  y$w_buff0_used = TRUE;
  __VERIFIER_assert(!(y$w_buff1_used && y$w_buff0_used));
  y$r_buff1_thd0 = y$r_buff0_thd0;
  y$r_buff1_thd1 = y$r_buff0_thd1;
  y$r_buff1_thd2 = y$r_buff0_thd2;
  y$r_buff1_thd3 = y$r_buff0_thd3;
  y$r_buff1_thd4 = y$r_buff0_thd4;
  y$r_buff0_thd3 = TRUE;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  weak$$choice0 = __VERIFIER_nondet_bool();
  weak$$choice2 = __VERIFIER_nondet_bool();
  y$flush_delayed = weak$$choice2;
  y$mem_tmp = y;
  y = !y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y : (y$w_buff0_used && y$r_buff0_thd3 ? y$w_buff0 : y$w_buff1);
  y$w_buff0 = weak$$choice2 ? y$w_buff0 : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$w_buff0 : (y$w_buff0_used && y$r_buff0_thd3 ? y$w_buff0 : y$w_buff0));
  y$w_buff1 = weak$$choice2 ? y$w_buff1 : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$w_buff1 : (y$w_buff0_used && y$r_buff0_thd3 ? y$w_buff1 : y$w_buff1));
  y$w_buff0_used = weak$$choice2 ? y$w_buff0_used : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$w_buff0_used : (y$w_buff0_used && y$r_buff0_thd3 ? FALSE : y$w_buff0_used));
  y$w_buff1_used = weak$$choice2 ? y$w_buff1_used : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$w_buff1_used : (y$w_buff0_used && y$r_buff0_thd3 ? FALSE : FALSE));
  y$r_buff0_thd3 = weak$$choice2 ? y$r_buff0_thd3 : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$r_buff0_thd3 : (y$w_buff0_used && y$r_buff0_thd3 ? FALSE : y$r_buff0_thd3));
  y$r_buff1_thd3 = weak$$choice2 ? y$r_buff1_thd3 : (!y$w_buff0_used || !y$r_buff0_thd3 && !y$w_buff1_used || !y$r_buff0_thd3 && !y$r_buff1_thd3 ? y$r_buff1_thd3 : (y$w_buff0_used && y$r_buff0_thd3 ? FALSE : FALSE));
  __unbuffered_p2_EAX = y;
  y = y$flush_delayed ? y$mem_tmp : y;
  y$flush_delayed = FALSE;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  weak$$choice0 = __VERIFIER_nondet_bool();
  weak$$choice2 = __VERIFIER_nondet_bool();
  z$flush_delayed = weak$$choice2;
  z$mem_tmp = z;
  z = !z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z : (z$w_buff0_used && z$r_buff0_thd3 ? z$w_buff0 : z$w_buff1);
  z$w_buff0 = weak$$choice2 ? z$w_buff0 : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$w_buff0 : (z$w_buff0_used && z$r_buff0_thd3 ? z$w_buff0 : z$w_buff0));
  z$w_buff1 = weak$$choice2 ? z$w_buff1 : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$w_buff1 : (z$w_buff0_used && z$r_buff0_thd3 ? z$w_buff1 : z$w_buff1));
  z$w_buff0_used = weak$$choice2 ? z$w_buff0_used : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$w_buff0_used : (z$w_buff0_used && z$r_buff0_thd3 ? FALSE : z$w_buff0_used));
  z$w_buff1_used = weak$$choice2 ? z$w_buff1_used : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$w_buff1_used : (z$w_buff0_used && z$r_buff0_thd3 ? FALSE : FALSE));
  z$r_buff0_thd3 = weak$$choice2 ? z$r_buff0_thd3 : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$r_buff0_thd3 : (z$w_buff0_used && z$r_buff0_thd3 ? FALSE : z$r_buff0_thd3));
  z$r_buff1_thd3 = weak$$choice2 ? z$r_buff1_thd3 : (!z$w_buff0_used || !z$r_buff0_thd3 && !z$w_buff1_used || !z$r_buff0_thd3 && !z$r_buff1_thd3 ? z$r_buff1_thd3 : (z$w_buff0_used && z$r_buff0_thd3 ? FALSE : FALSE));
  __unbuffered_p2_EBX = z;
  z = z$flush_delayed ? z$mem_tmp : z;
  z$flush_delayed = FALSE;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  y = y$w_buff0_used && y$r_buff0_thd3 ? y$w_buff0 : (y$w_buff1_used && y$r_buff1_thd3 ? y$w_buff1 : y);
  y$w_buff0_used = y$w_buff0_used && y$r_buff0_thd3 ? FALSE : y$w_buff0_used;
  y$w_buff1_used = y$w_buff0_used && y$r_buff0_thd3 || y$w_buff1_used && y$r_buff1_thd3 ? FALSE : y$w_buff1_used;
  y$r_buff0_thd3 = y$w_buff0_used && y$r_buff0_thd3 ? FALSE : y$r_buff0_thd3;
  y$r_buff1_thd3 = y$w_buff0_used && y$r_buff0_thd3 || y$w_buff1_used && y$r_buff1_thd3 ? FALSE : y$r_buff1_thd3;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}



void * P3(void *arg)
{
  __VERIFIER_atomic_begin();
  z$w_buff1 = z$w_buff0;
  z$w_buff0 = 1;
  z$w_buff1_used = z$w_buff0_used;
  z$w_buff0_used = TRUE;
  __VERIFIER_assert(!(z$w_buff1_used && z$w_buff0_used));
  z$r_buff1_thd0 = z$r_buff0_thd0;
  z$r_buff1_thd1 = z$r_buff0_thd1;
  z$r_buff1_thd2 = z$r_buff0_thd2;
  z$r_buff1_thd3 = z$r_buff0_thd3;
  z$r_buff1_thd4 = z$r_buff0_thd4;
  z$r_buff0_thd4 = TRUE;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  weak$$choice0 = __VERIFIER_nondet_bool();
  weak$$choice2 = __VERIFIER_nondet_bool();
  z$flush_delayed = weak$$choice2;
  z$mem_tmp = z;
  z = !z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z : (z$w_buff0_used && z$r_buff0_thd4 ? z$w_buff0 : z$w_buff1);
  z$w_buff0 = weak$$choice2 ? z$w_buff0 : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$w_buff0 : (z$w_buff0_used && z$r_buff0_thd4 ? z$w_buff0 : z$w_buff0));
  z$w_buff1 = weak$$choice2 ? z$w_buff1 : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$w_buff1 : (z$w_buff0_used && z$r_buff0_thd4 ? z$w_buff1 : z$w_buff1));
  z$w_buff0_used = weak$$choice2 ? z$w_buff0_used : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$w_buff0_used : (z$w_buff0_used && z$r_buff0_thd4 ? FALSE : z$w_buff0_used));
  z$w_buff1_used = weak$$choice2 ? z$w_buff1_used : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$w_buff1_used : (z$w_buff0_used && z$r_buff0_thd4 ? FALSE : FALSE));
  z$r_buff0_thd4 = weak$$choice2 ? z$r_buff0_thd4 : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$r_buff0_thd4 : (z$w_buff0_used && z$r_buff0_thd4 ? FALSE : z$r_buff0_thd4));
  z$r_buff1_thd4 = weak$$choice2 ? z$r_buff1_thd4 : (!z$w_buff0_used || !z$r_buff0_thd4 && !z$w_buff1_used || !z$r_buff0_thd4 && !z$r_buff1_thd4 ? z$r_buff1_thd4 : (z$w_buff0_used && z$r_buff0_thd4 ? FALSE : FALSE));
  __unbuffered_p3_EAX = z;
  z = z$flush_delayed ? z$mem_tmp : z;
  z$flush_delayed = FALSE;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_p3_EBX = a;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  y = y$w_buff0_used && y$r_buff0_thd4 ? y$w_buff0 : (y$w_buff1_used && y$r_buff1_thd4 ? y$w_buff1 : y);
  y$w_buff0_used = y$w_buff0_used && y$r_buff0_thd4 ? FALSE : y$w_buff0_used;
  y$w_buff1_used = y$w_buff0_used && y$r_buff0_thd4 || y$w_buff1_used && y$r_buff1_thd4 ? FALSE : y$w_buff1_used;
  y$r_buff0_thd4 = y$w_buff0_used && y$r_buff0_thd4 ? FALSE : y$r_buff0_thd4;
  y$r_buff1_thd4 = y$w_buff0_used && y$r_buff0_thd4 || y$w_buff1_used && y$r_buff1_thd4 ? FALSE : y$r_buff1_thd4;
  z = z$w_buff0_used && z$r_buff0_thd4 ? z$w_buff0 : (z$w_buff1_used && z$r_buff1_thd4 ? z$w_buff1 : z);
  z$w_buff0_used = z$w_buff0_used && z$r_buff0_thd4 ? FALSE : z$w_buff0_used;
  z$w_buff1_used = z$w_buff0_used && z$r_buff0_thd4 || z$w_buff1_used && z$r_buff1_thd4 ? FALSE : z$w_buff1_used;
  z$r_buff0_thd4 = z$w_buff0_used && z$r_buff0_thd4 ? FALSE : z$r_buff0_thd4;
  z$r_buff1_thd4 = z$w_buff0_used && z$r_buff0_thd4 || z$w_buff1_used && z$r_buff1_thd4 ? FALSE : z$r_buff1_thd4;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}



void fence()
{
  
}



void isync()
{
  
}



void lwfence()
{
  
}



int main()
{
  pthread_t t233;
  pthread_create(&t233, NULL, P0, NULL);
  pthread_t t234;
  pthread_create(&t234, NULL, P1, NULL);
  pthread_t t235;
  pthread_create(&t235, NULL, P2, NULL);
  pthread_t t236;
  pthread_create(&t236, NULL, P3, NULL);
  __VERIFIER_atomic_begin();
  main$tmp_guard0 = __unbuffered_cnt == 4;
  __VERIFIER_atomic_end();
  assume_abort_if_not(main$tmp_guard0);
  __VERIFIER_atomic_begin();
  y = y$w_buff0_used && y$r_buff0_thd0 ? y$w_buff0 : (y$w_buff1_used && y$r_buff1_thd0 ? y$w_buff1 : y);
  y$w_buff0_used = y$w_buff0_used && y$r_buff0_thd0 ? FALSE : y$w_buff0_used;
  y$w_buff1_used = y$w_buff0_used && y$r_buff0_thd0 || y$w_buff1_used && y$r_buff1_thd0 ? FALSE : y$w_buff1_used;
  y$r_buff0_thd0 = y$w_buff0_used && y$r_buff0_thd0 ? FALSE : y$r_buff0_thd0;
  y$r_buff1_thd0 = y$w_buff0_used && y$r_buff0_thd0 || y$w_buff1_used && y$r_buff1_thd0 ? FALSE : y$r_buff1_thd0;
  z = z$w_buff0_used && z$r_buff0_thd0 ? z$w_buff0 : (z$w_buff1_used && z$r_buff1_thd0 ? z$w_buff1 : z);
  z$w_buff0_used = z$w_buff0_used && z$r_buff0_thd0 ? FALSE : z$w_buff0_used;
  z$w_buff1_used = z$w_buff0_used && z$r_buff0_thd0 || z$w_buff1_used && z$r_buff1_thd0 ? FALSE : z$w_buff1_used;
  z$r_buff0_thd0 = z$w_buff0_used && z$r_buff0_thd0 ? FALSE : z$r_buff0_thd0;
  z$r_buff1_thd0 = z$w_buff0_used && z$r_buff0_thd0 || z$w_buff1_used && z$r_buff1_thd0 ? FALSE : z$r_buff1_thd0;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  /* Program proven to be relaxed for X86, model checker says YES. */
  weak$$choice0 = __VERIFIER_nondet_bool();
  /* Program proven to be relaxed for X86, model checker says YES. */
  weak$$choice2 = __VERIFIER_nondet_bool();
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$flush_delayed = weak$$choice2;
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$mem_tmp = y;
  /* Program proven to be relaxed for X86, model checker says YES. */
  y = !y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y : (y$w_buff0_used && y$r_buff0_thd0 ? y$w_buff0 : y$w_buff1);
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$w_buff0 = weak$$choice2 ? y$w_buff0 : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$w_buff0 : (y$w_buff0_used && y$r_buff0_thd0 ? y$w_buff0 : y$w_buff0));
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$w_buff1 = weak$$choice2 ? y$w_buff1 : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$w_buff1 : (y$w_buff0_used && y$r_buff0_thd0 ? y$w_buff1 : y$w_buff1));
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$w_buff0_used = weak$$choice2 ? y$w_buff0_used : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$w_buff0_used : (y$w_buff0_used && y$r_buff0_thd0 ? FALSE : y$w_buff0_used));
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$w_buff1_used = weak$$choice2 ? y$w_buff1_used : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$w_buff1_used : (y$w_buff0_used && y$r_buff0_thd0 ? FALSE : FALSE));
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$r_buff0_thd0 = weak$$choice2 ? y$r_buff0_thd0 : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$r_buff0_thd0 : (y$w_buff0_used && y$r_buff0_thd0 ? FALSE : y$r_buff0_thd0));
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$r_buff1_thd0 = weak$$choice2 ? y$r_buff1_thd0 : (!y$w_buff0_used || !y$r_buff0_thd0 && !y$w_buff1_used || !y$r_buff0_thd0 && !y$r_buff1_thd0 ? y$r_buff1_thd0 : (y$w_buff0_used && y$r_buff0_thd0 ? FALSE : FALSE));
  /* Program proven to be relaxed for X86, model checker says YES. */
  main$tmp_guard1 = !(x == 2 && y == 2 && __unbuffered_p2_EAX == 2 && __unbuffered_p2_EBX == 0 && __unbuffered_p3_EAX == 1 && __unbuffered_p3_EBX == 0);
  /* Program proven to be relaxed for X86, model checker says YES. */
  y = y$flush_delayed ? y$mem_tmp : y;
  /* Program proven to be relaxed for X86, model checker says YES. */
  y$flush_delayed = FALSE;
  __VERIFIER_atomic_end();
  /* Program proven to be relaxed for X86, model checker says YES. */
  __VERIFIER_assert(main$tmp_guard1);
  return 0;
}

