typedef unsigned long size_t;
struct node {
   int h ;
   struct node *n ;
};
typedef struct node *List;
extern  __attribute__((__nothrow__)) void *( __attribute__((__leaf__)) malloc)(size_t __size )  __attribute__((__malloc__)) ;
extern int __VERIFIER_nondet_int() ;
int flag  =    1;
float harmonicMean(List l );
int main(void);
int __return_166;
float __return_419;
int __return_424;
float __return_409;
float __return_418;
int main()
{
List l ;
void *tmp ;
List temp ;
int size ;
int tmp___0 ;
int i ;
List next ;
void *tmp___1 ;
tmp = malloc(8);
l = (List )tmp;
temp = l;
if (((unsigned long)temp) == ((unsigned long)((void *)0)))
{
 __return_166 = -1;
label_166:; 
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_323:; 
if (i < size)
{
tmp___1 = malloc(8);
next = (List )tmp___1;
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
next->n = l;
l = next;
goto label_429;
}
else 
{
label_429:; 
i = i + 1;
goto label_323;
}
}
else 
{
{
List __tmp_1 = l;
List l = __tmp_1;
int neg ;
int length ;
float sum ;
neg = 0;
length = 0;
sum = 0.0;
if ((l->h) == 0)
{
 __return_419 = -1.0;
label_419:; 
}
else 
{
if ((l->h) < 0)
{
neg = 1;
goto label_387;
}
else 
{
label_387:; 
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
if ((l->h) < 0)
{
neg = 1;
goto label_387;
}
else 
{
goto label_387;
}
}
else 
{
if (neg == 0)
{
flag = (int)sum;
 __return_409 = ((float)length) / sum;
goto label_419;
}
else 
{
flag = (int)sum;
 __return_418 = ((float)length) / sum;
goto label_419;
}
}
}
}
 __return_424 = 0;
goto label_166;
}
}
}
}
