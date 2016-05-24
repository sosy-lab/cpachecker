struct fraction {
   int c ;
   int d ;
};
extern int __VERIFIER_nondet_int() ;
int flag  =    1;
int inter  ;
int gcd(int x , int y );
void reduceFraction(struct fraction frac );
void main(void);
int __return_325;
int __return_351;
int __return_374;
void main()
{
struct fraction frac ;
struct fraction frac2 ;
frac.c = __VERIFIER_nondet_int();
label_299:; 
frac.d = __VERIFIER_nondet_int();
if ((frac.d) == 0)
{
goto label_299;
}
else 
{
if ((frac.c) != 0)
{
frac2.c = frac.d;
frac2.d = frac.c;
{
struct fraction __tmp_1 = frac2;
struct fraction frac = __tmp_1;
{
int __tmp_2 = frac.c;
int __tmp_3 = frac.d;
int x = __tmp_2;
int y = __tmp_3;
 __return_325 = 1;
}
inter = __return_325;
flag = inter;
frac.c = (frac.c) / inter;
flag = inter;
frac.d = (frac.d) / inter;
}
{
struct fraction __tmp_4 = frac;
struct fraction frac = __tmp_4;
{
int __tmp_5 = frac.c;
int __tmp_6 = frac.d;
int x = __tmp_5;
int y = __tmp_6;
 __return_351 = 1;
}
inter = __return_351;
flag = inter;
frac.c = (frac.c) / inter;
flag = inter;
frac.d = (frac.d) / inter;
}
label_385:; 
return 1;
}
else 
{
{
struct fraction __tmp_7 = frac;
struct fraction frac = __tmp_7;
{
int __tmp_8 = frac.c;
int __tmp_9 = frac.d;
int x = __tmp_8;
int y = __tmp_9;
 __return_374 = 1;
}
inter = __return_374;
flag = inter;
frac.c = (frac.c) / inter;
flag = inter;
frac.d = (frac.d) / inter;
}
goto label_385;
}
}
}
