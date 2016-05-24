void main();
void main()
{
int flag;
int z;
int y;
int x;
x = 0;
if (flag == 1)
{
x = 1;
label_93:; 
if (y > 0)
{
x = x * y;
y = y - 1;
goto label_93;
}
else 
{
return 1;
}
}
else 
{
label_58:; 
if (y > 0)
{
label_61:; 
if (flag == 1)
{
label_68:; 
x = x * y;
label_75:; 
goto label_51;
}
else 
{
label_69:; 
x = x - y;
label_71:; 
label_51:; 
y = y - 1;
goto label_58;
}
}
else 
{
label_62:; 
label_65:; 
return 1;
}
}
}
