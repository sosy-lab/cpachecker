extern int __VERIFIER_nondet_int();
int main(void);
int __return_940;
int __return_852;
int main()
{
int pattern[8] ;
int pathend[3] ;
int pathlim ;
int i ;
int anymeta ;
int tmp ;
int pos ;
unsigned long __cil_tmp9 ;
unsigned long __cil_tmp10 ;
unsigned long __cil_tmp11 ;
unsigned long __cil_tmp12 ;
unsigned long __cil_tmp13 ;
int *__cil_tmp14 ;
int *__cil_tmp15 ;
unsigned long __cil_tmp16 ;
unsigned long __cil_tmp17 ;
unsigned long __cil_tmp18 ;
unsigned long __cil_tmp19 ;
unsigned long __cil_tmp20 ;
pathlim = 2;
anymeta = 0;
label_856:; 
label_858:; 
label_860:; 
i = 0;
label_862:; 
label_865:; 
label_867:; 
label_869:; 
int __CPAchecker_TMP_0;
label_871:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
label_873:; 
if (__CPAchecker_TMP_0 == 0)
{
label_876:; 
label_928:; 
label_930:; 
int __CPAchecker_TMP_1;
label_932:; 
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
label_934:; 
if (__CPAchecker_TMP_1 == 0)
{
label_937:; 
label_942:; 
goto label_856;
}
else 
{
label_938:; 
 __return_940 = 0;
goto label_852;
}
}
else 
{
label_877:; 
label_879:; 
pos = 0;
label_881:; 
__cil_tmp9 = i * 4UL;
label_883:; 
__cil_tmp10 = ((unsigned long)pattern) + __cil_tmp9;
label_885:; 
anymeta = *((int *)__cil_tmp10);
label_887:; 
__cil_tmp11 = (unsigned long)pathlim;
label_889:; 
__cil_tmp12 = 0UL;
label_891:; 
__cil_tmp13 = ((unsigned long)pathend) + __cil_tmp12;
label_893:; 
__cil_tmp14 = (int *)__cil_tmp13;
label_895:; 
__cil_tmp15 = __cil_tmp14 + i;
label_897:; 
__cil_tmp16 = (unsigned long)__cil_tmp15;
label_899:; 
if (__cil_tmp16 >= __cil_tmp11)
{
label_902:; 
 __return_852 = 1;
label_852:; 
return 1;
}
else 
{
label_903:; 
label_905:; 
pos = 0;
label_907:; 
__cil_tmp17 = i * 4UL;
label_909:; 
__cil_tmp18 = ((unsigned long)pattern) + __cil_tmp17;
label_911:; 
tmp = *((int *)__cil_tmp18);
label_913:; 
pos = 1;
label_915:; 
__cil_tmp19 = i * 4UL;
label_917:; 
__cil_tmp20 = ((unsigned long)pathend) + __cil_tmp19;
label_919:; 
*((int *)__cil_tmp20) = tmp;
label_921:; 
i = i + 1;
return 1;
}
}
}
