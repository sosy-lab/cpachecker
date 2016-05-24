extern int __VERIFIER_nondet_int(void);
int flag = 0;
int main();
int __return_322;
int __return_326;
int __return_325;
int __return_319;
int main()
{
int a = __VERIFIER_nondet_int();
a = __VERIFIER_nondet_int();
int b = __VERIFIER_nondet_int();
b = __VERIFIER_nondet_int();
int c = __VERIFIER_nondet_int();
c = __VERIFIER_nondet_int();
int d = __VERIFIER_nondet_int();
d = __VERIFIER_nondet_int();
int id, utriag, ltriag, triag, unknown;

unknown = 0;
triag = unknown;
ltriag = triag;
utriag = ltriag;
id = utriag;
if (c == 0)
{
triag = 1;
if (!(b == 0))
{
utriag = 1;
flag = c;
 __return_322 = 1;
goto label_326;
}
else 
{
if (a == 1)
{
if (!(d == 1))
{
goto label_273;
}
else 
{
id = 1;
goto label_273;
}
}
else 
{
label_273:; 
ltriag = 1;
utriag = 1;
if (!(id == 0))
{
label_285:; 
flag = c;
flag = b;
 __return_326 = 1;
label_326:; 
return 1;
}
else 
{
goto label_285;
}
}
}
}
else 
{
if (!(b == 0))
{
unknown = 1;
 __return_325 = 1;
goto label_326;
}
else 
{
triag = 1;
ltriag = 1;
 __return_319 = 1;
goto label_326;
}
}
}
