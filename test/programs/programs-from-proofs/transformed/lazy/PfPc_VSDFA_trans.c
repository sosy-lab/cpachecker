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
label_95:; 
x = x - 1;
i = 1;
x = x + 1;
i = 0;
goto label_95;
}
else 
{
y = 5;
x = y;
x = x + 1;
label_96:; 
x = x - 1;
i = 1;
x = x + 1;
i = 0;
goto label_96;
}
}
