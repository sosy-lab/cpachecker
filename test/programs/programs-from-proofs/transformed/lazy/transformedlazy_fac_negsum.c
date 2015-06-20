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
label_34:; 
if (y > 0)
{
x = x * y;
y = y - 1;
goto label_34;
}
else 
{
return 1;
}
}
else 
{
label_51:; 
if (y > 0)
{
label_54:; 
if (flag == 1)
{
label_61:; 
x = x * y;
label_68:; 
goto label_70;
}
else 
{
label_62:; 
x = x - y;
label_64:; 
label_70:; 
y = y - 1;
label_72:; 
goto label_51;
}
}
else 
{
label_55:; 
label_58:; 
return 1;
}
}
}
