typedef unsigned int size_t;
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern int __VERIFIER_nondet_int();
int flag=0;
int is_relaxed_prefix(
  int pat[], unsigned long pat_length,
  int a[], unsigned long a_length);
int main();
int __return_228;
int __return_247;
int __return_214;
int __return_246;
int __return_200;
int __return_245;
int __return_186;
int __return_244;
int __return_172;
int __return_177;
int __return_243;
int __return_163;
int __return_242;
int __return_159;
int main()
{
int SIZE = 6;
unsigned long pat_len = SIZE, a_len = SIZE;
int *pat=malloc(sizeof(int)*pat_len);
int *a=malloc(sizeof(int)*a_len);
SIZE = 6;
pat_len = SIZE;
a_len = SIZE;
pat = malloc(4 * pat_len);
a = malloc(4 * a_len);
{
int __tmp_1[] = pat;
unsigned long __tmp_2 = pat_len;
int __tmp_3[] = a;
unsigned long __tmp_4 = a_len;
int pat[] = __tmp_1;
unsigned long pat_length = __tmp_2;
int a[] = __tmp_3;
unsigned long a_length = __tmp_4;
int shift=0;
int i=0;
shift = 0;
i = 0;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_228 = 0;
}
else 
{
i = i + 1;
flag = i;
goto label_209;
}
 __return_247 = 0;
return 1;
}
else 
{
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
flag = i;
label_209:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_214 = 0;
}
else 
{
i = i + 1;
flag = i;
goto label_195;
}
 __return_246 = 0;
return 1;
}
else 
{
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
flag = i;
label_195:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_200 = 0;
}
else 
{
i = i + 1;
flag = i;
goto label_181;
}
 __return_245 = 0;
return 1;
}
else 
{
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
flag = i;
label_181:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_186 = 0;
}
else 
{
i = i + 1;
flag = i;
goto label_167;
}
 __return_244 = 0;
return 1;
}
else 
{
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
flag = i;
label_167:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_172 = 0;
}
else 
{
i = i + 1;
 __return_177 = 1;
}
goto label_238;
label_238:; 
 __return_243 = 0;
return 1;
}
else 
{
i = i + 1;
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
 __return_163 = 1;
}
else 
{
i = i + 1;
 __return_159 = 1;
}
label_240:; 
 __return_242 = 0;
return 1;
goto label_240;
}
}
}
}
}
}
}
