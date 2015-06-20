extern void init();
extern void lock();
extern void unlock();
void main();
void main()
{
int n;
init();
lock();
int lastLock = 0;
int i = 1;
label_74:; 
if (i < n)
{
unlock();
i = i + 1;
if (i < n)
{
lock();
lastLock = i;
i = i + 1;
goto label_74;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
