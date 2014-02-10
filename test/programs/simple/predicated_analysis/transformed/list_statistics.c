typedef unsigned long size_t;
struct node {
   int h ;
   struct node *n ;
};
typedef struct node *List;
extern int __VERIFIER_nondet_int() ;
extern  __attribute__((__nothrow__)) void *( __attribute__((__leaf__)) malloc)(size_t __size )  __attribute__((__malloc__)) ;
int num  ;
int num2  ;
int flag1  ;
int sum  ;
float mean  ;
int flag  =    1;
int inter  ;
List successor  ;
List newHead  ;
int is_empty(List head );
List extract_even(List head );
float calc_mean(List head );
float variance(List head );
int main(void);
int __return_986;
List __return_1216;
List __return_1219;
List __return_1179;
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
List le ;
List tmp___2 ;
float m ;
float tmp___3 ;
int tmp___4 ;
tmp = malloc(sizeof(struct node ));
l = (List )tmp;
temp = l;
if (((unsigned long)temp) == ((unsigned long)((void *)0)))
{
 __return_986 = -1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_1127:; 
if (i < size)
{
tmp___1 = malloc(sizeof(struct node ));
next = (List )tmp___1;
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
next->n = l;
l = next;
goto label_1255;
}
else 
{
label_1255:; 
i = i + 1;
goto label_1127;
}
}
else 
{
{
List __tmp_1 = l;
List head = __tmp_1;
void *tmp ;
newHead = (List )((void *)0);
label_1140:; 
if (((head->h) % 2) == 0)
{
successor = newHead;
tmp = malloc(sizeof(struct node ));
newHead = (List )tmp;
if (((unsigned long)newHead) == ((unsigned long)((void *)0)))
{
newHead = successor;
goto label_1151;
}
else 
{
newHead->n = successor;
label_1169:; 
if (((unsigned long)head) != ((unsigned long)((void *)0)))
{
if (((head->h) % 2) == 0)
{
successor = newHead;
tmp = malloc(sizeof(struct node ));
newHead = (List )tmp;
if (((unsigned long)newHead) == ((unsigned long)((void *)0)))
{
newHead = successor;
label_1193:; 
label_1197:; 
if (((unsigned long)head) != ((unsigned long)((void *)0)))
{
if (((head->h) % 2) == 0)
{
successor = newHead;
tmp = malloc(sizeof(struct node ));
newHead = (List )tmp;
if (((unsigned long)newHead) == ((unsigned long)((void *)0)))
{
newHead = successor;
goto label_1193;
}
else 
{
newHead->n = successor;
goto label_1195;
}
}
else 
{
goto label_1197;
}
}
else 
{
 __return_1216 = newHead;
}
tmp___2 = __return_1216;
return 1;
}
else 
{
newHead->n = successor;
label_1195:; 
label_1199:; 
if (((unsigned long)head) != ((unsigned long)((void *)0)))
{
if (((head->h) % 2) == 0)
{
successor = newHead;
tmp = malloc(sizeof(struct node ));
newHead = (List )tmp;
if (((unsigned long)newHead) == ((unsigned long)((void *)0)))
{
newHead = successor;
goto label_1195;
}
else 
{
newHead->n = successor;
goto label_1195;
}
}
else 
{
goto label_1199;
}
}
else 
{
 __return_1219 = newHead;
}
tmp___2 = __return_1219;
return 1;
}
}
else 
{
goto label_1169;
}
}
else 
{
 __return_1179 = newHead;
}
tmp___2 = __return_1179;
return 1;
}
}
else 
{
label_1151:; 
goto label_1140;
}
}
}
}
}
