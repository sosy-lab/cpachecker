extern int __VERIFIER_nondet_int() ;
int flag  =    0;
static int gdTcl_UtfToUniChar(char *str , int *chPtr );
void gdImageStringFTEx(char *string );
int main(void);
int __return_1321;
int __return_1423;
int main()
{
char in[3] ;
in[2] = 0;
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
goto label_1419;
}
else 
{
if (encoding < 0)
{
goto label_1419;
}
else 
{
next = 0;
label_1242:; 
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
goto label_1386;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_1386;
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
 __return_1321 = 1;
}
len = __return_1321;
next = next + len;
goto label_1327;
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
label_1363:; 
next = next + 1;
goto label_1382;
}
else 
{
next = next + 1;
goto label_1327;
}
}
else 
{
next = next + 1;
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
goto label_1396;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_1396;
}
else 
{
flag = next;
c = (unsigned char)(*(string + next));
if (161 <= ((int)c))
{
if (((int)c) <= 254)
{
next = next + 1;
label_1382:; 
label_1396:; 
goto label_1419;
}
else 
{
goto label_1363;
}
}
else 
{
next = next + 1;
goto label_1382;
}
}
}
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
label_1419:; 
}
else 
{
label_1327:; 
label_1386:; 
goto label_1419;
}
 __return_1423 = 0;
return 1;
}
else 
{
goto label_1242;
}
}
}
}
}
}
}
}
}
