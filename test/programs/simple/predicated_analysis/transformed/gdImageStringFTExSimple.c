extern int __VERIFIER_nondet_int() ;
int flag  =    0;
static int gdTcl_UtfToUniChar(char *str , int *chPtr );
void gdImageStringFTEx(char *string );
int main(void);
int __return_3473;
int __return_3417;
int __return_3256;
int main()
{
char in[3] ;
in[2] = (char)0;
{
char *__tmp_1 = in;
char *string = __tmp_1;
int next ;
int encoding ;
int ch ;
int len ;
unsigned char c ;
encoding = __VERIFIER_nondet_int();
if (encoding > 2)
{
label_3372:; 
}
else 
{
if (encoding < 0)
{
goto label_3372;
}
else 
{
next = 0;
label_3227:; 
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
label_3393:; 
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
label_3343:; 
goto label_3372;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_3343;
}
else 
{
if (encoding == 0)
{
{
char *__tmp_2 = string + next;
int *__tmp_3 = &ch;
char *str = __tmp_2;
int *chPtr = __tmp_3;
 __return_3417 = 1;
}
len = __return_3417;
next = next + len;
label_3464:; 
goto label_3343;
}
else 
{
if (encoding == 1)
{
flag = next;
c = (unsigned char)(*(string + next));
if (161 <= ((int)c))
{
if (((int)c) <= 254)
{
next = next + 1;
goto label_3459;
}
else 
{
goto label_3444;
}
}
else 
{
label_3439:; 
next = next + 1;
label_3459:; 
goto label_3464;
}
}
else 
{
if (encoding == 2)
{
flag = next;
ch = ((int)(*(string + next))) & 255;
next = next + 1;
if (ch >= 161)
{
goto label_3329;
}
else 
{
goto label_3464;
}
}
else 
{
goto label_3381;
}
}
}
}
}
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_3393;
}
else 
{
if (encoding == 0)
{
{
char *__tmp_4 = string + next;
int *__tmp_5 = &ch;
char *str = __tmp_4;
int *chPtr = __tmp_5;
 __return_3256 = 1;
}
len = __return_3256;
next = next + len;
label_3381:; 
goto label_3393;
}
else 
{
if (encoding == 1)
{
flag = next;
c = (unsigned char)(*(string + next));
if (161 <= ((int)c))
{
if (((int)c) <= 254)
{
next = next + 1;
label_3444:; 
goto label_3439;
}
else 
{
goto label_3286;
}
}
else 
{
label_3286:; 
next = next + 1;
goto label_3381;
}
}
else 
{
if (encoding == 2)
{
flag = next;
ch = ((int)(*(string + next))) & 255;
next = next + 1;
if (ch >= 161)
{
flag = next;
ch = (ch * 256) + (((int)(*(string + next))) & 255);
next = next + 1;
label_3329:; 
goto label_3372;
}
else 
{
goto label_3381;
}
}
else 
{
goto label_3227;
}
}
}
}
}
}
}
 __return_3473 = 0;
return 1;
}
}
