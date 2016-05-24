typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void * malloc(size_t __size);
void free(void *__ptr);
int flag = 1;
void exit(int p);
void __VERIFIER_assert(int cond);
struct cell {
    int data;
    struct cell* next;
};
struct cell *S;
int pc1 = 1;
int pc4 = 1;
static struct cell *t1 = 0;
static struct cell *x1 = 0;
void push();
struct cell* garbage;
static struct cell *t4 = 0;
static struct cell *x4 = 0;
void pop();
int main();
static int res4;
int __return_262;
int __return_317;
int main()
{
label_353:; 
label_356:; 
label_358:; 
int __CPAchecker_TMP_0;
label_360:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
label_362:; 
if (__CPAchecker_TMP_0 == 0)
{
label_366:; 
label_370:; 
label_373:; 
S = 0;
label_376:; 
t1 = 0;
label_379:; 
x1 = 0;
label_382:; 
t4 = 0;
label_385:; 
x4 = 0;
label_388:; 
 __return_262 = 0;
label_262:; 
return 1;
}
else 
{
label_368:; 
int __CPAchecker_TMP_1;
label_263:; 
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
{
int __CPAchecker_TMP_0 = pc4;
pc4 = pc4 + 1;
t4 = S;
}
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
S = 0;
t1 = 0;
x1 = 0;
t4 = 0;
x4 = 0;
 __return_317 = 0;
return 1;
}
else 
{
int __CPAchecker_TMP_1;
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
{
int __CPAchecker_TMP_0 = pc4;
pc4 = pc4 + 1;
pc4 = 1;
}
goto label_353;
}
else 
{
{
int __CPAchecker_TMP_0 = pc1;
pc1 = pc1 + 1;
x1 = malloc(8);
{
int __tmp_1 = 0;
int p = __tmp_1;
return 1;
}
}
}
}
}
else 
{
{
int __CPAchecker_TMP_0 = pc1;
pc1 = pc1 + 1;
x1 = malloc(8);
{
int __tmp_2 = 0;
int p = __tmp_2;
return 1;
}
}
}
}
}
