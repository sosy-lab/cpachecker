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
goto label_171;
}
else 
{
z = -x;
label_171:; 
z = z + 10;
goto label_178;
}
}
else 
{
if (y >= 0)
{
s = 1;
label_150:; 
if (!(x >= y))
{
label_97:; 
label_178:; 
return 1;
}
else 
{
label_153:; 
if (x == 0)
{
goto label_97;
}
else 
{
label_158:; 
label_160:; 
z = z + x;
label_162:; 
label_123:; 
x = x - s;
goto label_150;
}
}
}
else 
{
s = -y;
label_128:; 
if (x >= y)
{
label_131:; 
if (x == 0)
{
goto label_97;
}
else 
{
label_136:; 
label_138:; 
z = z + 1;
label_140:; 
label_143:; 
x = x - s;
label_145:; 
goto label_128;
}
}
else 
{
goto label_97;
}
}
}
}
