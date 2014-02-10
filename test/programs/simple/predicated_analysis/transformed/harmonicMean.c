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
int __return_268;
float __return_416;
int __return_488;
float __return_474;
int __return_492;
float __return_458;
int __return_490;
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
tmp = malloc(sizeof(struct node ));
l = (List )tmp;
temp = l;
if (((unsigned long)temp) == ((unsigned long)((void *)0)))
{
 __return_268 = -1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_389:; 
if (i < size)
{
tmp___1 = malloc(sizeof(struct node ));
next = (List )tmp___1;
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
next->n = l;
l = next;
goto label_497;
}
else 
{
label_497:; 
i = i + 1;
goto label_389;
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
sum = (float)0;
if ((l->h) == 0)
{
 __return_416 = (float)(-1);
}
else 
{
if ((l->h) < 0)
{
neg = 1;
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
label_436:; 
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
neg = 1;
goto label_469;
}
else 
{
flag = (int)sum;
 __return_474 = ((float)length) / sum;
}
 __return_492 = 0;
return 1;
}
else 
{
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
label_434:; 
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
if ((l->h) < 0)
{
neg = 1;
label_469:; 
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
goto label_436;
}
else 
{
flag = l->h;
sum = (float)(1 / (l->h));
length = length + 1;
goto label_434;
}
}
else 
{
flag = (int)sum;
 __return_458 = ((float)length) / sum;
}
 __return_490 = 0;
return 1;
}
}
 __return_488 = 0;
return 1;
}
}
}
}
