void main();
void main()
{
int y;
int i=0;
int x = 0;
if (y < 0)
{
y = 0;
label_141:; 
x = y;
x = x + 1;
x = x - 1;
i = 1;
label_156:; 
x = x + 1;
i = 0;
if (i == 1)
{
x = x + 1;
i = 0;
return 1;
}
else 
{
x = x - 1;
i = 1;
goto label_156;
}
}
else 
{
y = 5;
goto label_141;
}
}
