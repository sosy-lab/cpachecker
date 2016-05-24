extern int __VERIFIER_nondet_int();
int main(void);
int __return_819;
int __return_731;
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
label_735:; 
label_737:; 
label_739:; 
i = 0;
label_741:; 
label_744:; 
label_746:; 
label_748:; 
int __CPAchecker_TMP_0;
label_750:; 
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
label_752:; 
if (__CPAchecker_TMP_0 == 0)
{
label_755:; 
label_807:; 
label_809:; 
int __CPAchecker_TMP_1;
label_811:; 
__CPAchecker_TMP_1 = __VERIFIER_nondet_int();
label_813:; 
if (__CPAchecker_TMP_1 == 0)
{
label_816:; 
label_821:; 
goto label_735;
}
else 
{
label_817:; 
 __return_819 = 0;
goto label_731;
}
}
else 
{
label_756:; 
label_758:; 
pos = 0;
label_760:; 
__cil_tmp9 = i * 4UL;
label_762:; 
__cil_tmp10 = ((unsigned long)pattern) + __cil_tmp9;
label_764:; 
anymeta = *((int *)__cil_tmp10);
label_766:; 
__cil_tmp11 = (unsigned long)pathlim;
label_768:; 
__cil_tmp12 = 0UL;
label_770:; 
__cil_tmp13 = ((unsigned long)pathend) + __cil_tmp12;
label_772:; 
__cil_tmp14 = (int *)__cil_tmp13;
label_774:; 
__cil_tmp15 = __cil_tmp14 + i;
label_776:; 
__cil_tmp16 = (unsigned long)__cil_tmp15;
label_778:; 
if (__cil_tmp16 >= __cil_tmp11)
{
label_781:; 
 __return_731 = 1;
label_731:; 
return 1;
}
else 
{
label_782:; 
label_784:; 
pos = 0;
label_786:; 
__cil_tmp17 = i * 4UL;
label_788:; 
__cil_tmp18 = ((unsigned long)pattern) + __cil_tmp17;
label_790:; 
tmp = *((int *)__cil_tmp18);
label_792:; 
pos = 1;
label_794:; 
__cil_tmp19 = i * 4UL;
label_796:; 
__cil_tmp20 = ((unsigned long)pathend) + __cil_tmp19;
label_798:; 
*((int *)__cil_tmp20) = tmp;
label_800:; 
i = i + 1;
return 1;
}
}
}
