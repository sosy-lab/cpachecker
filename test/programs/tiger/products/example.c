

/*
extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "mapsum4.c", 6, __extension__ __PRETTY_FUNCTION__); })); }*/
extern int __VERIFIER_nondet_int();
/*
int mapsum (int x[1])
{
  int i;
  long long ret;
  ret = 0;
  return ret;
}
*/
int main ()
{
  int x[1];
  int temp;
  int ret;
  //int ret2;
 // int ret5;

  for (int t = 0; t < 1;) {
    x[t] = __VERIFIER_nondet_int();
if(t > 0){
t++;
}
  }
int z = 0;

 // ret = 1;

 // temp=x[0];x[0] = x[1]; x[1] = temp;
 //ret2 = 1;	
//temp=x[0];

 for(int i =0 ; i<1 -1; i++){
    x[i] = x[i+1];
 }
  //x[1 -1] = temp;
  //ret5 = 1;

//  if(ret != ret2 || ret !=ret5){
 //   {reach_error();}
  //}
  return 1;
}

