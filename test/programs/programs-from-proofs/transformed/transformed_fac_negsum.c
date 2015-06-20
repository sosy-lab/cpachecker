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
label_49:; 
if (y > 0)
{
x = x * y;
y = y - 1;
goto label_49;
}
else 
{
return 1;
}
}
else 
{
label_77:; 
if (y > 0)
{
label_80:; 
if (flag == 1)
{
label_87:; 
x = x * y;
label_94:; 
goto label_96;
}
else 
{
label_88:; 
x = x - y;
label_90:; 
label_96:; 
y = y - 1;
label_98:; 
goto label_77;
}
}
else 
{
label_81:; 
label_84:; 
return 1;
}
}
}
