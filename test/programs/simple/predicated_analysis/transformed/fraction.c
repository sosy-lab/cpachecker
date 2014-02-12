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
int __return_241;
int __return_273;
int __return_279;
int __return_213;
int __return_219;
void main()
{
struct fraction frac ;
struct fraction frac2 ;
frac.c = __VERIFIER_nondet_int();
label_189:; 
frac.d = __VERIFIER_nondet_int();
if ((frac.d) == 0)
{
goto label_189;
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
if (x != 0)
{
 __return_241 = 1;
}
else 
{
 __return_241 = 1;
}
inter = __return_241;
label_245:; 
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
if (x != 0)
{
 __return_273 = 1;
}
else 
{
 __return_273 = 1;
}
inter = __return_273;
label_277:; 
flag = inter;
frac.c = (frac.c) / inter;
flag = inter;
frac.d = (frac.d) / inter;
}
return 1;
}
}
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
if (x != 0)
{
 __return_213 = 1;
}
else 
{
 __return_213 = 1;
}
inter = __return_213;
label_217:; 
flag = inter;
frac.c = (frac.c) / inter;
flag = inter;
frac.d = (frac.d) / inter;
}
label_1:
return 1;
}
}
}
}
