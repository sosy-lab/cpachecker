void main();
void main()
{
int y;
int i=0;
int x = 0;
if (y < 0)
{
y = 0;
label_40:; 
x = y;
x = x + 1;
x = x - 1;
i = 1;
label_70:; 
x = x + 1;
i = 0;
x = x - 1;
i = 1;
goto label_70;
}
else 
{
y = 5;
goto label_40;
}
}
