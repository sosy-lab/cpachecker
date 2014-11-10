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
goto label_17;
}
else 
{
x = a;
label_17:; 
y = b * a;
if (y > x)
{
i = 0;
label_47:; 
if (b > 0)
{
x = x + y;
goto label_47;
}
else 
{
goto label_56;
}
}
else 
{
i = 1;
label_45:; 
if (b > 0)
{
goto label_45;
}
else 
{
label_56:; 
return 1;
}
}
}
}
