extern void __VERIFIER_assume(int);
extern unsigned long __VERIFIER_nondet_ulong(void);
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error() __attribute__ ((__noreturn__));

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
typedef unsigned int size_t;

extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern void free (void *__ptr) __attribute__ ((__nothrow__ , __leaf__));

int main()
{
  unsigned long pat_len = __VERIFIER_nondet_ulong(), a_len = __VERIFIER_nondet_ulong();


    unsigned long different = __VERIFIER_nondet_ulong();
    if(pat_len>a_len)
      different=pat_len-1;

    if(pat_len >= a_len){
	int i = 0;

}
  return 0;
}

