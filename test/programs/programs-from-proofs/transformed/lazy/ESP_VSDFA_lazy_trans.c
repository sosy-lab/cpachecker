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
if (!(dump == 0))
{
label_16:; 
f = 1;
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
x = 1;
goto label_66;
}
else 
{
x = 0;
label_66:; 
y = f;
label_75:; 
return 1;
}
}
else 
{
int __CPAchecker_TMP_0;
__CPAchecker_TMP_0 = __VERIFIER_nondet_int();
if (__CPAchecker_TMP_0 == 0)
{
x = 1;
goto label_68;
}
else 
{
x = 0;
label_68:; 
goto label_75;
}
}
}
