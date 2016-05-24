void main();
void main()
{
int s, x, y, z;

z = 0;
if (x < 0)
{
if (y < x)
{
z = -y;
goto label_117;
}
else 
{
z = -x;
label_117:; 
z = z + 10;
return 1;
}
}
else 
{
if (y >= 0)
{
s = 1;
label_101:; 
if (x >= y)
{
label_104:; 
if (x == 0)
{
goto label_105;
}
else 
{
label_108:; 
label_110:; 
z = z + x;
label_80:; 
x = x - s;
goto label_101;
}
}
else 
{
label_105:; 
return 1;
}
}
else 
{
s = -y;
label_85:; 
if (!(x >= y))
{
goto label_89;
}
else 
{
label_88:; 
if (x == 0)
{
label_89:; 
return 1;
}
else 
{
label_92:; 
label_94:; 
z = z + 1;
label_96:; 
label_98:; 
x = x - s;
label_83:; 
goto label_85;
}
}
}
}
}
