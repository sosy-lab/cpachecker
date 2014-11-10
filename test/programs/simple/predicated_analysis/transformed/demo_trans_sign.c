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
label_144:; 
y = y + 1;
if (y < 5)
{
x = x + 1;
goto label_144;
}
else 
{
goto label_81;
}
}
else 
{
label_81:; 
return 1;
}
}
else 
{
x = -1;
int z=y;
if (y < 5)
{
x = x - 1;
label_116:; 
y = y + 1;
if (y < 5)
{
x = x - 1;
goto label_116;
}
else 
{
goto label_81;
}
}
else 
{
goto label_81;
}
}
}
