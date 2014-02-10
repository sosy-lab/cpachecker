void main();
void main()
{
int n;
int flag;
int x=5;
int i=0;
if (flag == 0)
{
x = -1;
if (i < n)
{
x = x - 1;
label_140:; 
i = i + 1;
if (i < n)
{
x = x - 1;
goto label_140;
}
else 
{
goto label_132;
}
}
else 
{
label_132:; 
return 1;
}
}
else 
{
x = 1;
if (i < n)
{
x = x + 1;
label_143:; 
i = i + 1;
if (i < n)
{
x = x + 1;
goto label_143;
}
else 
{
goto label_132;
}
}
else 
{
goto label_132;
}
}
}
