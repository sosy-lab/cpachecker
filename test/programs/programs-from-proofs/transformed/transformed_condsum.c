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
label_146:; 
i = i + 1;
label_148:; 
label_137:; 
if (i < n)
{
x = x - 1;
goto label_146;
}
else 
{
goto label_126;
}
}
else 
{
label_126:; 
return 1;
}
}
else 
{
x = 1;
if (i < n)
{
x = x + 1;
label_131:; 
i = i + 1;
label_133:; 
label_121:; 
if (i < n)
{
x = x + 1;
goto label_131;
}
else 
{
goto label_126;
}
}
else 
{
goto label_126;
}
}
}
