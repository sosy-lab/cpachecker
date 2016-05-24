typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void * malloc(size_t __size);
extern int __VERIFIER_nondet_int();
int flag = 1;
void main();
void main()
{
int time=0;
int* p = malloc(sizeof(int));
p = malloc(4);
int r=__VERIFIER_nondet_int();
r = __VERIFIER_nondet_int();
if (p != 0)
{
flag = p;
time = *p;
goto label_48;
}
else 
{
label_48:; 
label_49:; 
if (r <= 0)
{
r = __VERIFIER_nondet_int();
goto label_49;
}
else 
{
return 1;
}
}
}
