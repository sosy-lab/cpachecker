void main();
void main()
{
int y;
if (y < 0)
{
y = 0;
goto label_121;
}
else 
{
label_121:; 
int x=y;
x = x + 1;
int i=0;
x = x - 1;
i = 1;
label_140:; 
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
goto label_140;
}
}
}
