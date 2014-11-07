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
label_116:; 
i = i + 1;
if (i < n)
{
x = x - 1;
goto label_116;
}
else 
{
goto label_90;
}
}
else 
{
goto label_90;
}
}
else 
{
x = 1;
if (i < n)
{
x = x + 1;
label_97:; 
i = i + 1;
if (i < n)
{
x = x + 1;
goto label_97;
}
else 
{
goto label_90;
}
}
else 
{
label_90:; 
return 1;
}
}
}