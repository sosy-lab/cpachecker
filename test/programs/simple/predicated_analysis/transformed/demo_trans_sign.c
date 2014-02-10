void main();
void main()
{
int y;
int x=5;
if (y > 1)
{
x = 1;
int z=y;
label_197:; 
if (y < 5)
{
x = x + 1;
y = y + 1;
goto label_197;
}
else 
{
label_212:; 
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
label_218:; 
y = y + 1;
if (y < 5)
{
x = x - 1;
goto label_218;
}
else 
{
goto label_212;
}
}
else 
{
goto label_212;
}
}
}
