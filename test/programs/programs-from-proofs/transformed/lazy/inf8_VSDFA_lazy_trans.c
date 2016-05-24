extern int __VERIFIER_nondet_int(void);
int flag = 0;
int main();
int __return_251;
int __return_255;
int __return_254;
int __return_248;
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
 __return_251 = 1;
goto label_255;
}
else 
{
if (a == 1)
{
if (!(d == 1))
{
goto label_202;
}
else 
{
id = 1;
goto label_202;
}
}
else 
{
label_202:; 
ltriag = 1;
utriag = 1;
if (!(id == 0))
{
label_214:; 
flag = c;
flag = b;
 __return_255 = 1;
label_255:; 
return 1;
}
else 
{
goto label_214;
}
}
}
}
else 
{
if (!(b == 0))
{
unknown = 1;
 __return_254 = 1;
goto label_255;
}
else 
{
triag = 1;
ltriag = 1;
 __return_248 = 1;
goto label_255;
}
}
}
