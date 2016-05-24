void main();
void main()
{
int a;
int b;
int y;
int i;
int x=0;
if (a < 0)
{
x = -a;
goto label_115;
}
else 
{
x = a;
label_115:; 
y = b * a;
if (y > x)
{
i = 0;
label_126:; 
if (b > 0)
{
x = x + y;
goto label_126;
}
else 
{
label_137:; 
return 1;
}
}
else 
{
i = 1;
label_128:; 
if (b > 0)
{
goto label_128;
}
else 
{
goto label_137;
}
}
}
}
