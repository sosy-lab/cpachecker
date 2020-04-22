
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
      result = num2;
   }
 
   return result; 
}
int maxWrong(int num1, int num2) {

   /* local variable declaration */
   int result;
 
   if (num1 > num2){
      result = -num1;
   }
   else{
      result = num2;
   }
 
   return result; 
}

int main() 
{ 
   /* local variable definition */
   int a = __VERIFIER_nondet_int();
   int b = __VERIFIER_nondet_int();
   int ret;
   int wrongRet;
 
   /* calling a function to get max value */
   ret = max(a, b);
   wrongRet = maxWrong(a,b);
 __VERIFIER_assert(ret == wrongRet);
 
   return 0;
} 
