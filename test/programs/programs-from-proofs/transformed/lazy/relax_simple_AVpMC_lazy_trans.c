typedef unsigned int size_t;
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern int __VERIFIER_nondet_int();
int flag=0;
int is_relaxed_prefix(
  int pat[], unsigned long pat_length,
  int a[], unsigned long a_length);
int main();
int __return_441;
int __return_477;
int __return_426;
int __return_438;
int __return_436;
int __return_419;
int __return_417;
int __return_395;
int __return_407;
int __return_475;
int __return_405;
int __return_388;
int __return_386;
int __return_364;
int __return_376;
int __return_473;
int __return_374;
int __return_357;
int __return_355;
int __return_333;
int __return_345;
int __return_471;
int __return_343;
int __return_326;
int __return_324;
int __return_305;
int __return_315;
int __return_469;
int __return_313;
int __return_298;
int __return_296;
int __return_287;
int __return_467;
int __return_285;
int __return_278;
int __return_262;
int __return_244;
int __return_226;
int __return_208;
int __return_190;
int __return_172;
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
 __return_441 = 0;
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
 __return_426 = 0;
goto label_419;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_389;
}
else 
{
if (pat_length > a_length)
{
 __return_438 = 0;
}
else 
{
 __return_436 = 1;
}
goto label_442;
goto label_442;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_419 = 0;
label_419:; 
}
else 
{
 __return_417 = 1;
goto label_190;
}
goto label_442;
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
label_389:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_395 = 0;
goto label_388;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_358;
}
else 
{
if (pat_length > a_length)
{
 __return_407 = 0;
}
else 
{
 __return_405 = 1;
}
label_446:; 
 __return_475 = 0;
return 1;
goto label_446;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_388 = 0;
label_388:; 
}
else 
{
 __return_386 = 1;
goto label_208;
}
goto label_446;
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
label_358:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_364 = 0;
goto label_357;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_327;
}
else 
{
if (pat_length > a_length)
{
 __return_376 = 0;
}
else 
{
 __return_374 = 1;
}
label_449:; 
 __return_473 = 0;
return 1;
goto label_449;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_357 = 0;
label_357:; 
}
else 
{
 __return_355 = 1;
goto label_226;
}
goto label_449;
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
label_327:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_333 = 0;
goto label_326;
}
else 
{
i = i + 1;
if (i < a_length)
{
flag = i;
goto label_299;
}
else 
{
if (pat_length > a_length)
{
 __return_345 = 0;
}
else 
{
 __return_343 = 1;
}
label_452:; 
 __return_471 = 0;
return 1;
goto label_452;
}
}
}
else 
{
if (pat_length > a_length)
{
 __return_326 = 0;
label_326:; 
}
else 
{
 __return_324 = 1;
goto label_244;
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
label_299:; 
flag = i - shift;
if ((pat[i]) != (a[i - shift]))
{
 __return_305 = 0;
goto label_298;
}
else 
{
i = i + 1;
if (pat_length > a_length)
{
 __return_315 = 0;
}
else 
{
 __return_313 = 1;
}
label_455:; 
 __return_469 = 0;
return 1;
goto label_455;
}
}
else 
{
if (pat_length > a_length)
{
 __return_298 = 0;
label_298:; 
}
else 
{
 __return_296 = 1;
goto label_262;
}
goto label_455;
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
 __return_287 = 0;
}
else 
{
 __return_285 = 1;
goto label_278;
}
label_458:; 
 __return_467 = 0;
return 1;
}
else 
{
i = i + 1;
if (pat_length > a_length)
{
goto label_275;
}
else 
{
label_275:; 
 __return_278 = 1;
label_278:; 
}
goto label_458;
}
}
else 
{
if (pat_length > a_length)
{
goto label_259;
}
else 
{
label_259:; 
 __return_262 = 1;
label_262:; 
}
goto label_455;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_241;
}
else 
{
label_241:; 
 __return_244 = 1;
label_244:; 
}
goto label_452;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_223;
}
else 
{
label_223:; 
 __return_226 = 1;
label_226:; 
}
goto label_449;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_205;
}
else 
{
label_205:; 
 __return_208 = 1;
label_208:; 
}
goto label_446;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_187;
}
else 
{
label_187:; 
 __return_190 = 1;
label_190:; 
}
goto label_442;
}
}
}
else 
{
if (pat_length > a_length)
{
goto label_169;
}
else 
{
label_169:; 
 __return_172 = 1;
}
goto label_442;
}
}
label_442:; 
 __return_477 = 0;
return 1;
}
}
