extern int __VERIFIER_nondet_int();
void *__builtin_alloca(unsigned long  ) ;
int flag  =    0;
int main(void);
int __return_624;
int __return_625;
int __return_623;
int main()
{
int BASE_SZ ;
int *outfile ;
unsigned long __lengthofoutfile ;
void *tmp ;
int c1 ;
int nchar ;
int out ;
unsigned long __cil_tmp9 ;
int *__cil_tmp10 ;
int *__cil_tmp11 ;
int *__cil_tmp12 ;
int __CPAchecker_TMP_0;
int __CPAchecker_TMP_1;
int __CPAchecker_TMP_2;
int __CPAchecker_TMP_3;
int __CPAchecker_TMP_4;
int __CPAchecker_TMP_5;
BASE_SZ = 2;
__lengthofoutfile = (unsigned long)BASE_SZ;
__cil_tmp9 = 4UL * __lengthofoutfile;
tmp = __builtin_alloca(__cil_tmp9);
outfile = (int *)tmp;
nchar = 0;
out = 0;
label_420:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
goto label_434;
}
else 
{
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
nchar = nchar + 1;
flag = out;
label_461:; 
__cil_tmp11 = outfile + out;
*__cil_tmp11 = c1;
out = out + 1;
__CPAchecker_TMP_5 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_5 == 0)
{
goto label_471;
}
else 
{
label_489:; 
flag = out;
__cil_tmp12 = outfile + out;
*__cil_tmp12 = 1;
out = out + 1;
 __return_624 = 0;
return 1;
}
}
else 
{
__CPAchecker_TMP_2 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_2 == 0)
{
__CPAchecker_TMP_3 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_3 == 0)
{
__CPAchecker_TMP_4 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_4 == 0)
{
nchar = nchar + 1;
flag = out;
label_452:; 
__cil_tmp10 = outfile + out;
*__cil_tmp10 = c1;
out = out + 1;
label_471:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
goto label_489;
}
else 
{
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
nchar = nchar + 1;
flag = out;
__cil_tmp11 = outfile + out;
*__cil_tmp11 = c1;
out = out + 1;
__CPAchecker_TMP_5 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_5 == 0)
{
goto label_526;
}
else 
{
label_594:; 
flag = out;
__cil_tmp12 = outfile + out;
*__cil_tmp12 = 1;
out = out + 1;
 __return_625 = 0;
return 1;
}
}
else 
{
__CPAchecker_TMP_2 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_2 == 0)
{
__CPAchecker_TMP_3 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_3 == 0)
{
__CPAchecker_TMP_4 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_4 == 0)
{
nchar = nchar + 1;
flag = out;
__cil_tmp10 = outfile + out;
*__cil_tmp10 = c1;
out = out + 1;
label_526:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
goto label_594;
}
else 
{
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
nchar = nchar + 1;
goto label_594;
}
else 
{
__CPAchecker_TMP_2 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_2 == 0)
{
__CPAchecker_TMP_3 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_3 == 0)
{
__CPAchecker_TMP_4 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_4 == 0)
{
nchar = nchar + 1;
goto label_594;
}
else 
{
goto label_594;
}
}
else 
{
out = 0;
label_573:; 
nchar = 0;
label_551:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
goto label_566;
}
else 
{
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_1 == 0)
{
nchar = nchar + 1;
flag = out;
goto label_461;
}
else 
{
__CPAchecker_TMP_2 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_2 == 0)
{
__CPAchecker_TMP_3 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_3 == 0)
{
__CPAchecker_TMP_4 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_4 == 0)
{
nchar = nchar + 1;
flag = out;
goto label_452;
}
else 
{
goto label_566;
}
}
else 
{
out = 0;
goto label_573;
}
}
else 
{
label_566:; 
flag = out;
goto label_610;
}
}
}
}
}
else 
{
goto label_594;
}
}
}
}
else 
{
goto label_489;
}
}
else 
{
out = 0;
goto label_496;
}
}
else 
{
goto label_489;
}
}
}
}
else 
{
goto label_434;
}
}
else 
{
out = 0;
label_496:; 
nchar = 0;
label_441:; 
goto label_420;
}
}
else 
{
label_434:; 
flag = out;
label_610:; 
__cil_tmp12 = outfile + out;
*__cil_tmp12 = 1;
out = out + 1;
 __return_623 = 0;
return 1;
}
}
}
}
