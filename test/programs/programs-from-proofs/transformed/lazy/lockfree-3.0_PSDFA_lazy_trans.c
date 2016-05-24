typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void * malloc(size_t __size);
void free(void *__ptr);
int flag = 1;
struct cell {
    int data;
    struct cell* next;
};
struct cell *S;
int pc1 = 1;
int pc4 = 1;
void exit(int p);
void push();
struct cell* garbage;
void pop();
void main();
static struct cell *t1 = 0;
static struct cell *x1 = 0;
static struct cell *t4 = 0;
static struct cell *x4 = 0;
static struct cell *static__push__x1;
static struct cell *static__pop__t4;
static int res4;
void main()
{
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
label_2408:; 
return 1;
}
else 
{
label_2322:; 
int __CPAchecker_TMP_1;
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
{
int __CPAchecker_TMP_0 = pc4;
pc4 = pc4 + 1;
static__pop__t4 = S;
}
label_2390:; 
if (!(1 != pc4))
{
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
goto label_2408;
}
else 
{
goto label_2322;
}
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
if (__CPAchecker_TMP_0 == 2)
{
pc4 = 1;
goto label_2469;
}
else 
{
label_2469:; 
}
goto label_2390;
}
}
else 
{
{
int __CPAchecker_TMP_0 = pc1;
pc1 = pc1 + 1;
static__push__x1 = malloc(8);
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
static__push__x1 = malloc(8);
{
int __tmp_2 = 0;
int p = __tmp_2;
return 1;
}
}
}
}
}
