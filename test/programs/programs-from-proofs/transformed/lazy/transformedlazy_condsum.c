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
label_110:; 
i = i + 1;
label_112:; 
label_101:; 
if (i < n)
{
x = x - 1;
goto label_110;
}
else 
{
goto label_92;
}
}
else 
{
label_92:; 
return 1;
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
label_99:; 
label_87:; 
if (i < n)
{
x = x + 1;
goto label_97;
}
else 
{
goto label_92;
}
}
else 
{
goto label_92;
}
}
}
