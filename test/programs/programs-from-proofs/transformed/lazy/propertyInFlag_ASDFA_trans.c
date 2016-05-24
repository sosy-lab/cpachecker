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
goto label_49;
}
else 
{
x = a;
label_49:; 
y = b * a;
if (y > x)
{
i = 0;
label_57:; 
if (b > 0)
{
x = x + y;
goto label_57;
}
else 
{
return 1;
}
}
else 
{
i = 1;
label_58:; 
if (b > 0)
{
goto label_58;
}
else 
{
return 1;
}
}
}
}
