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
int __return_327;
List __return_270;
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
tmp = malloc(8);
l = (List )tmp;
temp = l;
if (((unsigned long)temp) == ((unsigned long)((void *)0)))
{
 __return_327 = -1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_240:; 
label_242:; 
if (i < size)
{
label_245:; 
label_277:; 
tmp___1 = malloc(8);
label_283:; 
next = (List )tmp___1;
label_290:; 
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
label_295:; 
next->n = l;
label_303:; 
l = next;
label_309:; 
goto label_301;
}
else 
{
label_296:; 
label_301:; 
i = i + 1;
label_318:; 
goto label_240;
}
}
else 
{
label_246:; 
label_248:; 
{
List __tmp_1 = l;
List head = __tmp_1;
label_251:; 
label_253:; 
void *tmp ;
label_255:; 
newHead = (List )((void *)0);
label_257:; 
label_260:; 
label_262:; 
if (((unsigned long)head) != ((unsigned long)((void *)0)))
{
label_265:; 
label_275:; 
if (((head->h) % 2) == 0)
{
label_280:; 
successor = newHead;
label_288:; 
tmp = malloc(8);
label_292:; 
newHead = (List )tmp;
label_298:; 
if (((unsigned long)newHead) == ((unsigned long)((void *)0)))
{
label_306:; 
newHead = successor;
label_320:; 
goto label_404;
}
else 
{
label_307:; 
newHead->n = successor;
label_311:; 
label_404:; 
goto label_363;
}
}
else 
{
label_281:; 
label_363:; 
goto label_260;
}
}
else 
{
label_266:; 
label_268:; 
 __return_270 = newHead;
label_270:; 
}
tmp___2 = __return_270;
return 1;
}
}
}
}
