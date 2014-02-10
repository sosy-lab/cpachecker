void main();
void main()
{
int y;
int x=5;
if (y > 1)
{
x = y;
goto label_11;
}
else 
{
label_11:; 
if (x == y)
{
x = x - 1;
goto label_23;
}
else 
{
label_23:; 
return 1;
}
}
}
