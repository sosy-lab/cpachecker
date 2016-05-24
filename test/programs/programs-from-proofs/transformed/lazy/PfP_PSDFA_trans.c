void main();
void main()
{
int y;
if (y < 0)
{
y = 0;
goto label_72;
}
else 
{
label_72:; 
int x=y;
x = x + 1;
int i=0;
x = x - 1;
i = 1;
label_86:; 
x = x + 1;
i = 0;
x = x - 1;
i = 1;
goto label_86;
}
}
