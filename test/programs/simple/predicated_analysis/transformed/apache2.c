extern int __VERIFIER_nondet_int() ;
int flag  =    0;
void escape_absolute_uri(char *uri , int scheme );
int main(void);
int __return_2504;
int main()
{
char uri[11] ;
int scheme ;
uri[10] = (char)0;
scheme = 6;
{
char *__tmp_1 = uri;
int __tmp_2 = scheme;
char *uri = __tmp_1;
int scheme = __tmp_2;
int cp ;
char *token[3] ;
int c ;
int cond ;
int tmp ;
tmp = __VERIFIER_nondet_int();
cond = tmp;
if (cond == 0)
{
cp = scheme;
flag = cp - 1;
if (((int)(*(uri + (cp - 1)))) == 47)
{
flag = cp;
if (((int)(*(uri + cp))) != 47)
{
cp = cp + 1;
scheme = cp;
cond = __VERIFIER_nondet_int();
if (cond == 0)
{
goto label_2406;
}
else 
{
c = 0;
token[0] = uri;
flag = cp;
if (((int)(*(uri + cp))) == 63)
{
c = c + 1;
token[c] = (uri + cp) + 1;
flag = cp;
*(uri + cp) = (char)0;
goto label_2385;
}
else 
{
label_2385:; 
cp = cp + 1;
goto label_2397;
}
}
}
else 
{
cp = cp + 1;
flag = cp;
if (((int)(*(uri + cp))) != 47)
{
cp = cp + 1;
scheme = cp;
cond = __VERIFIER_nondet_int();
if (cond == 0)
{
goto label_2406;
}
else 
{
c = 0;
token[0] = uri;
label_2397:; 
flag = cp;
if (((int)(*(uri + cp))) == 63)
{
c = c + 1;
token[c] = (uri + cp) + 1;
flag = cp;
*(uri + cp) = (char)0;
goto label_2460;
}
else 
{
label_2460:; 
cp = cp + 1;
goto label_2466;
}
}
}
else 
{
cp = cp + 1;
flag = cp;
if (((int)(*(uri + cp))) != 47)
{
cp = cp + 1;
scheme = cp;
cond = __VERIFIER_nondet_int();
if (cond == 0)
{
goto label_2406;
}
else 
{
c = 0;
token[0] = uri;
label_2466:; 
if (c < 2)
{
flag = cp;
if (((int)(*(uri + cp))) == 63)
{
c = c + 1;
token[c] = (uri + cp) + 1;
flag = cp;
*(uri + cp) = (char)0;
goto label_2492;
}
else 
{
label_2492:; 
cp = cp + 1;
goto label_2500;
}
}
else 
{
label_2500:; 
goto label_2425;
}
}
}
else 
{
cp = cp + 1;
flag = cp;
if (((int)(*(uri + cp))) != 47)
{
goto label_2425;
}
else 
{
cp = cp + 1;
goto label_2425;
}
}
}
}
}
else 
{
label_2406:; 
goto label_2425;
}
}
else 
{
label_2425:; 
}
 __return_2504 = 0;
return 1;
}
}
