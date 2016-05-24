extern int __VERIFIER_nondet_int(void);
int flag = 0;
int main();
int __return_272;
int __return_274;
int __return_273;
int __return_275;
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
label_213:; 
if (b == 0)
{
if (a == 1)
{
if (d == 1)
{
id = 1;
goto label_233;
}
else 
{
goto label_233;
}
}
else 
{
label_233:; 
ltriag = 1;
utriag = 1;
if (id == 0)
{
goto label_245;
}
else 
{
label_245:; 
flag = c;
flag = b;
 __return_272 = 1;
return 1;
}
}
}
else 
{
if (b == 0)
{
ltriag = 1;
goto label_224;
}
else 
{
utriag = 1;
flag = c;
 __return_274 = 1;
return 1;
}
}
}
else 
{
if (b == 0)
{
triag = 1;
if (c == 0)
{
goto label_213;
}
else 
{
ltriag = 1;
label_224:; 
 __return_273 = 1;
return 1;
}
}
else 
{
unknown = 1;
 __return_275 = 1;
return 1;
}
}
}
