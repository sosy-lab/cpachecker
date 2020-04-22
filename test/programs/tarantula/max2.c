
extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

int max(int num1, int num2) {

   /* local variable declaration */
   int result;
 
   if (num1 > num2){
      result = num1;
   }
   else{
      result = -num2;
   }
 
   return result; 
}

int main() 
{ 
   int a = 1;
   int b = 2;
   int ret;
 
   /* calling a function to get max value */
   ret = max(a, b);
   
 __VERIFIER_assert(ret == 2);
 
   return 0;
} 
