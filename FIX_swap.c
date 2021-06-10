#include <stdio.h>
#include <assert.h>
extern void __VERIFIER_assume();
extern _Bool __VERIFIER_nondet_bool();
int __VERIFIER_nondet_int();
int main();
 int main()
 {
 int first;
 first = __VERIFIER_nondet_int();
 int second;
 second = __VERIFIER_nondet_int();
 int firstCopy = first;
 int secondCopy = second;
 int temp = first;
 temp = first;
 first = second;
 second = temp;
 if (first == firstCopy)
 {
label_0:;
 if (firstCopy != secondCopy)
 {
 ERROR:; return 1;
label_2:;
 abort();
 }
 else 
 {
 goto label_1;
 }
 }
 else 
 {
 if (second == secondCopy)
 {
 goto label_0;
 }
 else 
 {
 }
label_1:;
 EXIT:; return 0;
 goto label_2;
 }
 }

