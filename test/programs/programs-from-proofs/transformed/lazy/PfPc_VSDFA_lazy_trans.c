void main();
void main()
{
int y;
int i=0;
int x = 0;
if (y < 0)
{
y = 0;
x = y;
x = x + 1;
label_72:; 
x = x - 1;
i = 1;
x = x + 1;
i = 0;
goto label_72;
}
else 
{
y = 5;
x = y;
x = x + 1;
label_45:; 
x = x - 1;
i = 1;
if (!(i == 1))
{
return 1;
}
else 
{
x = x + 1;
i = 0;
goto label_45;
}
}
}
