extern int __VERIFIER_nondet_int(void);
int flag = 0;
int main();
int __return_282;
int __return_284;
int __return_285;
int __return_283;
int __return_286;
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
if (b == 0)
{
if (a == 1)
{
if (d == 1)
{
id = 1;
ltriag = 1;
utriag = 1;
if (utriag == ltriag)
{
flag = c;
flag = b;
goto label_266;
}
else 
{
label_266:; 
 __return_282 = 1;
return 1;
}
}
else 
{
goto label_224;
}
}
else 
{
label_224:; 
ltriag = 1;
utriag = 1;
if (utriag == ltriag)
{
flag = c;
flag = b;
goto label_263;
}
else 
{
label_263:; 
 __return_284 = 1;
return 1;
}
}
}
else 
{
utriag = 1;
flag = c;
 __return_285 = 1;
return 1;
}
}
else 
{
if (b == 0)
{
triag = 1;
ltriag = 1;
 __return_283 = 1;
return 1;
}
else 
{
unknown = 1;
 __return_286 = 1;
return 1;
}
}
}
