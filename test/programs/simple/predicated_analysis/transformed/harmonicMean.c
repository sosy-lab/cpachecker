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
int __return_309;
float __return_303;
float __return_280;
float __return_304;
int __return_308;
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
 __return_309 = -1;
label_309:; 
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
size = tmp___0;
i = 0;
label_178:; 
label_180:; 
if (i < size)
{
label_183:; 
label_220:; 
tmp___1 = malloc(8);
label_226:; 
next = (List )tmp___1;
label_230:; 
if (((unsigned long)next) != ((unsigned long)((void *)0)))
{
label_237:; 
next->n = l;
label_248:; 
l = next;
label_254:; 
goto label_246;
}
else 
{
label_238:; 
label_246:; 
i = i + 1;
label_260:; 
goto label_178;
}
}
else 
{
label_184:; 
label_186:; 
{
List __tmp_1 = l;
List l = __tmp_1;
label_189:; 
label_191:; 
int neg ;
label_193:; 
int length ;
label_195:; 
float sum ;
label_197:; 
neg = 0;
label_199:; 
length = 0;
label_201:; 
sum = 0.0;
label_203:; 
label_206:; 
label_208:; 
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
label_211:; 
label_218:; 
if ((l->h) == 0)
{
label_223:; 
 __return_303 = -1.0;
goto label_304;
}
else 
{
label_224:; 
label_228:; 
if ((l->h) < 0)
{
label_233:; 
neg = 1;
label_243:; 
goto label_288;
}
else 
{
label_234:; 
label_288:; 
flag = l->h;
label_293:; 
sum = (float)(1 / (l->h));
label_295:; 
length = length + 1;
label_297:; 
label_266:; 
if (((unsigned long)l) != ((unsigned long)((void *)0)))
{
if ((l->h) < 0)
{
neg = 1;
goto label_288;
}
else 
{
goto label_288;
}
}
else 
{
if (neg == 0)
{
flag = (int)sum;
 __return_280 = ((float)length) / sum;
goto label_304;
}
else 
{
flag = (int)sum;
 __return_304 = ((float)length) / sum;
label_304:; 
}
 __return_308 = 0;
goto label_309;
}
}
}
}
else 
{
label_212:; 
label_214:; 
return 1;
}
}
}
}
}
