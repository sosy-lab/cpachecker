void main();
void main()
{
int y;
int x=5;
if (y > 1)
{
x = 1;
int z=y;
if (y < 5)
{
x = x + 1;
label_129:; 
y = y + 1;
if (y < 5)
{
x = x + 1;
goto label_129;
}
else 
{
goto label_119;
}
}
else 
{
goto label_119;
}
}
else 
{
x = -1;
int z=y;
if (y < 5)
{
x = x - 1;
label_131:; 
y = y + 1;
if (y < 5)
{
x = x - 1;
goto label_131;
}
else 
{
goto label_119;
}
}
else 
{
label_119:; 
return 1;
}
}
}