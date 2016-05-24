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
label_108:; 
if (y > 0)
{
x = x * y;
y = y - 1;
goto label_108;
}
else 
{
return 1;
}
}
else 
{
label_135:; 
if (y > 0)
{
label_138:; 
if (flag == 1)
{
label_145:; 
x = x * y;
return 1;
}
else 
{
label_146:; 
x = x - y;
label_148:; 
label_125:; 
y = y - 1;
goto label_135;
}
}
else 
{
label_139:; 
label_142:; 
return 1;
}
}
}
