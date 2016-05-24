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
int __return_467;
float __return_446;
float __return_434;
float __return_447;
int __return_468;
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
 __return_467 = -1;
goto label_468;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_356:; 
if (i < size)
{
tmp___1 = malloc(8);
next = (List )tmp___1;
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
next->n = l;
l = next;
goto label_458;
}
else 
{
label_458:; 
i = i + 1;
goto label_356;
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
 __return_446 = -1.0;
goto label_447;
}
else 
{
if ((l->h) < 0)
{
neg = 1;
goto label_415;
}
else 
{
label_415:; 
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
if ((l->h) < 0)
{
neg = 1;
goto label_415;
}
else 
{
goto label_415;
}
}
else 
{
if (neg == 0)
{
flag = (int)sum;
 __return_434 = ((float)length) / sum;
goto label_447;
}
else 
{
flag = (int)sum;
 __return_447 = ((float)length) / sum;
label_447:; 
}
 __return_468 = 0;
label_468:; 
return 1;
}
}
}
}
}
}
}
