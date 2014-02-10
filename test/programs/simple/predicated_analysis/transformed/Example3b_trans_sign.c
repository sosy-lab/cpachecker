void main();
void main()
{
int x=0;
x = x + 1;
int i=0;
x = x - 1;
i = 1;
label_112:; 
x = x + 1;
i = 0;
if (i == 1)
{
x = x + 1;
i = 0;
return 1;
}
else 
{
x = x - 1;
i = 1;
goto label_112;
}
}
