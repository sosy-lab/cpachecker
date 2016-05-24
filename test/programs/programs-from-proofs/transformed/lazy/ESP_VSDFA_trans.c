extern int __VERIFIER_nondet_int(void);
void main();
void main()
{
int x;
int y=1;
int flag;
int f = 0;
int dump = __VERIFIER_nondet_int();
dump = __VERIFIER_nondet_int();
if (dump == 0)
{
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
x = 1;
goto label_85;
}
else 
{
x = 0;
label_85:; 
goto label_92;
}
}
else 
{
f = 1;
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
x = 1;
goto label_83;
}
else 
{
x = 0;
label_83:; 
y = f;
label_92:; 
return 1;
}
}
}
