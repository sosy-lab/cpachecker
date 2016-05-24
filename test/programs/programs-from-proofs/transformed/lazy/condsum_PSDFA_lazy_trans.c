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
label_108:; 
i = i + 1;
label_110:; 
label_99:; 
if (i < n)
{
x = x - 1;
goto label_108;
}
else 
{
goto label_61;
}
}
else 
{
label_61:; 
return 1;
}
}
else 
{
x = 1;
if (i < n)
{
x = x + 1;
label_93:; 
i = i + 1;
label_95:; 
label_84:; 
if (i < n)
{
x = x + 1;
goto label_93;
}
else 
{
goto label_61;
}
}
else 
{
goto label_61;
}
}
}
