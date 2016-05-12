typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void * malloc(size_t __size);
extern int __VERIFIER_nondet_int();


int flag = 1;

void main()
{
 int time=0;
 int* p = malloc(sizeof(int));
 int r=__VERIFIER_nondet_int();
 
 if(p!=0)
 {
   flag = p;
   time=*p;
 }
while(r<=0)
{
    if(time>0)
    {
        time++;
    }
    r=__VERIFIER_nondet_int();
}
if(time>0)
{
    flag=p;
    *p=time; 
}
}