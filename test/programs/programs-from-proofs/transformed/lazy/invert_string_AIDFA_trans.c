extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void __VERIFIER_assert(int cond);
char __VERIFIER_nondet_char();
int flag=0;
int main();
int main()
{
unsigned int max = 6;
char str1[max], str2[max];

int i, j;

int __CPAchecker_TMP_1;
int __CPAchecker_TMP_0;
max = 6;
i = 0;
label_411:; 
if (i < max)
{
label_405:; 
str1[i] = __VERIFIER_nondet_char();
label_407:; 
label_409:; 
i = i + 1;
goto label_411;
}
else 
{
str1[max - 1] = '\x0';
j = 0;
i = max - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
flag = j;
flag = i;
str2[j] = str1[i];
__CPAchecker_TMP_0 = j;
j = j + 1;
__CPAchecker_TMP_0;
i = i - 1;
j = max - 1;
i = 0;
flag = i;
flag = j;
{
int __tmp_1 = (str1[i]) == (str2[j]);
int cond = __tmp_1;
if (cond == 0)
{
__VERIFIER_error();
goto label_486;
}
else 
{
label_486:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
flag = i;
flag = j;
{
int __tmp_2 = (str1[i]) == (str2[j]);
int cond = __tmp_2;
if (cond == 0)
{
__VERIFIER_error();
goto label_504;
}
else 
{
label_504:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
flag = i;
flag = j;
{
int __tmp_3 = (str1[i]) == (str2[j]);
int cond = __tmp_3;
if (cond == 0)
{
__VERIFIER_error();
goto label_522;
}
else 
{
label_522:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
flag = i;
flag = j;
{
int __tmp_4 = (str1[i]) == (str2[j]);
int cond = __tmp_4;
if (cond == 0)
{
__VERIFIER_error();
goto label_540;
}
else 
{
label_540:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
flag = i;
flag = j;
{
int __tmp_5 = (str1[i]) == (str2[j]);
int cond = __tmp_5;
if (cond == 0)
{
__VERIFIER_error();
goto label_558;
}
else 
{
label_558:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
flag = i;
flag = j;
{
int __tmp_6 = (str1[i]) == (str2[j]);
int cond = __tmp_6;
if (cond == 0)
{
__VERIFIER_error();
goto label_576;
}
else 
{
label_576:; 
}
__CPAchecker_TMP_1 = j;
j = j - 1;
__CPAchecker_TMP_1;
i = i + 1;
return 1;
}
}
}
}
}
}
}
}
