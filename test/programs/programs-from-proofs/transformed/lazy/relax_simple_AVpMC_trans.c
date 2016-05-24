typedef unsigned int size_t;
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern int __VERIFIER_nondet_int();
int flag=0;
int is_relaxed_prefix(
  int pat[], unsigned long pat_length,
  int a[], unsigned long a_length);
int main();
int __return_451;
int __return_487;
int __return_436;
int __return_448;
int __return_446;
int __return_429;
int __return_427;
int __return_405;
int __return_417;
int __return_485;
int __return_415;
int __return_398;
int __return_396;
int __return_374;
int __return_386;
int __return_483;
int __return_384;
int __return_367;
int __return_365;
int __return_343;
int __return_355;
int __return_481;
int __return_353;
int __return_336;
int __return_334;
int __return_315;
int __return_325;
int __return_479;
int __return_323;
int __return_308;
int __return_306;
int __return_297;
int __return_477;
int __return_295;
int __return_288;
int __return_272;
int __return_254;
int __return_236;
int __return_218;
int __return_200;
int __return_182;
int main()
{
int SIZE = 6;
unsigned long pat_len = SIZE, a_len = SIZE;
int *pat=malloc(sizeof(int)*pat_len);
pat = malloc(4 * pat_len);
int *a=malloc(sizeof(int)*a_len);
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
if (pat_length > (a_length + 1))
{
 __return_451 = 0;
}
else 
{
int shift=0;
int i=0;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_436 = 0;
goto label_429;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_399;
}
else 
{
if (pat_length > a_length)
{
 __return_448 = 0;
}
else 
{
 __return_446 = 1;
}
goto label_452;
goto label_452;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_429 = 0;
label_429:; 
}
else 
{
 __return_427 = 1;
goto label_200;
}
goto label_452;
}
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (i < a_length)
{
flag = i;
label_399:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_405 = 0;
goto label_398;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_368;
}
else 
{
if (pat_length > a_length)
{
 __return_417 = 0;
}
else 
{
 __return_415 = 1;
}
label_456:; 
 __return_485 = 0;
return 1;
goto label_456;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_398 = 0;
label_398:; 
}
else 
{
 __return_396 = 1;
goto label_218;
}
goto label_456;
}
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (i < a_length)
{
flag = i;
label_368:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_374 = 0;
goto label_367;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_337;
}
else 
{
if (pat_length > a_length)
{
 __return_386 = 0;
}
else 
{
 __return_384 = 1;
}
label_459:; 
 __return_483 = 0;
return 1;
goto label_459;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_367 = 0;
label_367:; 
}
else 
{
 __return_365 = 1;
goto label_236;
}
goto label_459;
}
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (i < a_length)
{
flag = i;
label_337:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_343 = 0;
goto label_336;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_309;
}
else 
{
if (pat_length > a_length)
{
 __return_355 = 0;
}
else 
{
 __return_353 = 1;
}
label_462:; 
 __return_481 = 0;
return 1;
goto label_462;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_336 = 0;
label_336:; 
}
else 
{
 __return_334 = 1;
goto label_254;
}
goto label_462;
}
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (i < a_length)
{
flag = i;
label_309:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_315 = 0;
goto label_308;
}
else 
{
i = i + 1;
if (pat_length > a_length)
{
 __return_325 = 0;
}
else 
{
 __return_323 = 1;
}
label_465:; 
 __return_479 = 0;
return 1;
goto label_465;
}
}
else 
{
if (pat_length > a_length)
{
 __return_308 = 0;
label_308:; 
}
else 
{
 __return_306 = 1;
goto label_272;
}
goto label_465;
}
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
shift = 1;
i = i + 1;
if (pat_length > a_length)
{
 __return_297 = 0;
}
else 
{
 __return_295 = 1;
goto label_288;
}
label_468:; 
 __return_477 = 0;
return 1;
}
else 
{
i = i + 1;
if (pat_length > a_length)
{
goto label_285;
}
else 
{
label_285:; 
 __return_288 = 1;
label_288:; 
}
goto label_468;
}
}
else 
{
if (pat_length > a_length)
{
goto label_269;
}
else 
{
label_269:; 
 __return_272 = 1;
label_272:; 
}
goto label_465;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_251;
}
else 
{
label_251:; 
 __return_254 = 1;
label_254:; 
}
goto label_462;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_233;
}
else 
{
label_233:; 
 __return_236 = 1;
label_236:; 
}
goto label_459;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_215;
}
else 
{
label_215:; 
 __return_218 = 1;
label_218:; 
}
goto label_456;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_197;
}
else 
{
label_197:; 
 __return_200 = 1;
label_200:; 
}
goto label_452;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_179;
}
else 
{
label_179:; 
 __return_182 = 1;
}
goto label_452;
}
}
label_452:; 
 __return_487 = 0;
return 1;
}
}
