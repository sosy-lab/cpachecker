extern int __VERIFIER_nondet_int();
void *__builtin_alloca(unsigned long  ) ;
int flag1  =    0;
int flag2  =    0;
void main(void);
void main()
{
int i ;
int j ;
int bufsz ;
int *buffer ;
unsigned long __lengthofbuffer ;
void *tmp ;
int len ;
int *msg ;
unsigned long __lengthofmsg ;
void *tmp___0 ;
int limit ;
unsigned long __cil_tmp12 ;
unsigned long __cil_tmp13 ;
int __cil_tmp14 ;
int *__cil_tmp15 ;
int *__cil_tmp16 ;
int *__cil_tmp17 ;
int *__cil_tmp18 ;
int *__cil_tmp19 ;
int *__cil_tmp20 ;
int *__cil_tmp21 ;
int *__cil_tmp22 ;
int *__cil_tmp23 ;
int __CPAchecker_TMP_0;
i = 0;
j = 0;
bufsz = 6;
__lengthofbuffer = (unsigned long)bufsz;
__cil_tmp12 = 4UL * __lengthofbuffer;
tmp = __builtin_alloca(__cil_tmp12);
buffer = (int *)tmp;
len = bufsz + 5;
__lengthofmsg = (unsigned long)len;
__cil_tmp13 = 4UL * __lengthofmsg;
tmp___0 = __builtin_alloca(__cil_tmp13);
msg = (int *)tmp___0;
limit = bufsz - 4;
__cil_tmp14 = i + 1;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
flag1 = j;
flag2 = i;
__cil_tmp20 = buffer + j;
__cil_tmp21 = msg + i;
*__cil_tmp20 = *__cil_tmp21;
j = j + 1;
i = i + 1;
__cil_tmp14 = i + 1;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
flag1 = j;
flag2 = i;
__cil_tmp20 = buffer + j;
__cil_tmp21 = msg + i;
*__cil_tmp20 = *__cil_tmp21;
j = j + 1;
i = i + 1;
goto label_747;
}
else 
{
flag1 = j;
flag2 = i;
__cil_tmp15 = buffer + j;
__cil_tmp16 = msg + i;
*__cil_tmp15 = *__cil_tmp16;
j = j + 1;
i = i + 1;
flag1 = j;
flag2 = i;
__cil_tmp17 = buffer + j;
__cil_tmp18 = msg + i;
*__cil_tmp17 = *__cil_tmp18;
j = j + 1;
i = i + 1;
flag1 = j;
__cil_tmp19 = buffer + j;
*__cil_tmp19 = '.';
j = j + 1;
label_751:; 
goto label_751;
}
}
else 
{
flag1 = j;
flag2 = i;
__cil_tmp15 = buffer + j;
__cil_tmp16 = msg + i;
*__cil_tmp15 = *__cil_tmp16;
j = j + 1;
i = i + 1;
flag1 = j;
flag2 = i;
__cil_tmp17 = buffer + j;
__cil_tmp18 = msg + i;
*__cil_tmp17 = *__cil_tmp18;
j = j + 1;
i = i + 1;
flag1 = j;
__cil_tmp19 = buffer + j;
*__cil_tmp19 = '.';
j = j + 1;
label_747:; 
label_750:; 
label_765:; 
label_767:; 
label_769:; 
label_771:; 
label_773:; 
label_775:; 
label_777:; 
label_779:; 
label_781:; 
label_783:; 
label_785:; 
label_787:; 
goto label_765;
}
}
