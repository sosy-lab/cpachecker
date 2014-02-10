void main();
void main()
{
int a;
int b;
int y;
int i;
int x=0;
if (a < 0)
{
x = -a;
y = b * a;
if (y > x)
{
i = 0;
if (b > 0)
{
x = x + y;
label_349:; 
goto label_352;
}
else 
{
label_340:; 
return 1;
}
}
else 
{
i = 1;
label_310:; 
label_316:; 
if (b > 0)
{
goto label_316;
}
else 
{
goto label_340;
}
}
}
else 
{
x = a;
y = b * a;
if (y > x)
{
i = 0;
label_352:; 
if (b > 0)
{
if (i == 0)
{
x = x + y;
goto label_349;
}
else 
{
return 1;
}
}
else 
{
goto label_340;
}
}
else 
{
i = 1;
goto label_310;
}
}
}
