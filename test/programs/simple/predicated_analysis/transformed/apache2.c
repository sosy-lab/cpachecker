extern int __VERIFIER_nondet_int() ;
int flag  =    0;
void escape_absolute_uri(char *uri , int scheme );
int main(void);
int __return_2057;
int main()
{
char uri[11] ;
int scheme ;
uri[10] = 0;
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
goto label_1959;
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
*(uri + cp) = 0;
goto label_1938;
}
else 
{
label_1938:; 
cp = cp + 1;
goto label_1950;
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
goto label_1959;
}
else 
{
c = 0;
token[0] = uri;
label_1950:; 
flag = cp;
if (((int)(*(uri + cp))) == 63)
{
c = c + 1;
token[c] = (uri + cp) + 1;
flag = cp;
*(uri + cp) = 0;
goto label_2013;
}
else 
{
label_2013:; 
cp = cp + 1;
goto label_2019;
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
goto label_1959;
}
else 
{
c = 0;
token[0] = uri;
label_2019:; 
if (c < 2)
{
flag = cp;
if (((int)(*(uri + cp))) == 63)
{
c = c + 1;
token[c] = (uri + cp) + 1;
flag = cp;
*(uri + cp) = 0;
goto label_2045;
}
else 
{
label_2045:; 
cp = cp + 1;
goto label_2053;
}
}
else 
{
label_2053:; 
goto label_1978;
}
}
}
else 
{
cp = cp + 1;
flag = cp;
if (((int)(*(uri + cp))) != 47)
{
goto label_1978;
}
else 
{
cp = cp + 1;
goto label_1978;
}
}
}
}
}
else 
{
label_1959:; 
goto label_1978;
}
}
else 
{
label_1978:; 
}
 __return_2057 = 0;
return 1;
}
}
