extern int __VERIFIER_nondet_int() ;
int flag  =    0;
static int gdTcl_UtfToUniChar(char *str , int *chPtr );
void gdImageStringFTEx(char *string );
int main(void);
int __return_1969;
int __return_1153;
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
label_68:; 
}
else 
{
if (encoding < 0)
{
goto label_68;
}
else 
{
next = 0;
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
label_1173:; 
goto label_68;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_1173;
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
 __return_1153 = 1;
}
len = __return_1153;
next = next + len;
label_1215:; 
goto label_1173;
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
next = next + 1;
goto label_68;
}
else 
{
goto label_1183;
}
}
else 
{
label_1183:; 
next = next + 1;
goto label_1215;
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
goto label_68;
}
else 
{
goto label_1215;
}
}
else 
{
label_1793:; 
flag = next;
ch = (int)(*(string + next));
if (ch == 10)
{
next = next + 1;
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
label_1866:; 
goto label_68;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_1866;
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
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
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
return 1;
}
else 
{
return 1;
}
}
else 
{
label_1889:; 
flag = next;
ch = (int)(*(string + next));
if (ch == 13)
{
next = next + 1;
return 1;
}
else 
{
if (ch == 10)
{
next = next + 1;
goto label_1866;
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
return 1;
}
else 
{
return 1;
}
}
else 
{
return 1;
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
return 1;
}
else 
{
return 1;
}
}
else 
{
goto label_1889;
}
}
}
}
}
}
}
}
}
else 
{
if (encoding == 1)
{
flag = next;
c = (unsigned char)(*(string + next));
next = next + 1;
return 1;
}
else 
{
goto label_1793;
}
}
}
}
}
}
}
}
}
 __return_1969 = 0;
return 1;
}
}
