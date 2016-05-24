extern int __VERIFIER_nondet_int();
int main();
int __return_607;
int main()
{
int lk1;
int lk2;
int lk3;
int lk4;
int lk5;
int flag = 1;
int cond;
int p;
label_14:; 
cond = __VERIFIER_nondet_int();
p = __VERIFIER_nondet_int();
if (cond == 0)
{
 __return_607 = 0;
return 1;
}
else 
{
lk1 = 0;
lk2 = 0;
lk3 = 0;
lk4 = 0;
lk5 = 0;
if (p > 0)
{
lk1 = 1;
if (p > 1)
{
lk2 = 1;
if (p > 2)
{
lk3 = 1;
if (p > 3)
{
lk4 = 1;
if (p > 4)
{
lk5 = 1;
flag = lk1;
lk1 = 0;
flag = lk2;
lk2 = 0;
flag = lk3;
lk3 = 0;
flag = lk4;
lk4 = 0;
if (p > 4)
{
flag = lk5;
lk5 = 0;
goto label_530;
}
else 
{
goto label_530;
}
}
else 
{
flag = lk1;
lk1 = 0;
flag = lk2;
lk2 = 0;
flag = lk3;
lk3 = 0;
flag = lk4;
lk4 = 0;
goto label_518;
}
}
else 
{
flag = lk1;
lk1 = 0;
flag = lk2;
lk2 = 0;
flag = lk3;
lk3 = 0;
goto label_512;
}
}
else 
{
flag = lk1;
lk1 = 0;
flag = lk2;
lk2 = 0;
goto label_506;
}
}
else 
{
flag = lk1;
lk1 = 0;
goto label_500;
}
}
else 
{
label_500:; 
label_506:; 
label_512:; 
label_518:; 
label_530:; 
goto label_14;
}
}
}
