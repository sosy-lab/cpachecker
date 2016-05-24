int fib(int n);
int flag = 5;
int main();
int __return_275;
int __return_279;
int main()
{
{
int __tmp_1 = flag;
int n = __tmp_1;
int  i, Fnew, Fold, temp,ans;

Fnew = 1;
Fold = 0;
i = 2;
temp = Fnew;
Fnew = Fnew + Fold;
Fold = temp;
int __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
temp = Fnew;
Fnew = Fnew + Fold;
Fold = temp;
 __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
temp = Fnew;
Fnew = Fnew + Fold;
Fold = temp;
 __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
temp = Fnew;
Fnew = Fnew + Fold;
Fold = temp;
 __CPAchecker_TMP_0 = i;
i = i + 1;
__CPAchecker_TMP_0;
ans = Fnew;
 __return_275 = ans;
}
flag = __return_275;
 __return_279 = flag;
return 1;
}
