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
goto label_120;
}
else 
{
z = -x;
label_120:; 
z = z + 10;
return 1;
}
}
else 
{
if (y >= 0)
{
s = 1;
label_104:; 
if (x >= y)
{
label_107:; 
if (x == 0)
{
goto label_108;
}
else 
{
label_111:; 
label_113:; 
z = z + x;
label_83:; 
x = x - s;
goto label_104;
}
}
else 
{
label_108:; 
return 1;
}
}
else 
{
s = -y;
label_88:; 
if (!(x >= y))
{
goto label_92;
}
else 
{
label_91:; 
if (x == 0)
{
label_92:; 
return 1;
}
else 
{
label_95:; 
label_97:; 
z = z + 1;
label_99:; 
label_101:; 
x = x - s;
label_86:; 
goto label_88;
}
}
}
}
}
