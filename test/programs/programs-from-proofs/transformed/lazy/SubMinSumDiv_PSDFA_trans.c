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
goto label_217;
}
else 
{
z = -x;
label_217:; 
z = z + 10;
goto label_224;
}
}
else 
{
if (y >= 0)
{
s = 1;
label_196:; 
if (!(x >= y))
{
label_178:; 
label_224:; 
return 1;
}
else 
{
label_199:; 
if (x == 0)
{
goto label_178;
}
else 
{
label_204:; 
label_206:; 
z = z + x;
label_208:; 
label_168:; 
x = x - s;
goto label_196;
}
}
}
else 
{
s = -y;
label_173:; 
if (x >= y)
{
label_176:; 
if (x == 0)
{
goto label_178;
}
else 
{
label_182:; 
label_184:; 
z = z + 1;
label_186:; 
label_189:; 
x = x - s;
label_191:; 
goto label_173;
}
}
else 
{
goto label_178;
}
}
}
}
