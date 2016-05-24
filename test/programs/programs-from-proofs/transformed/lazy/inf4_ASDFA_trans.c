int main();
int __return_639;
int __return_652;
int __return_651;
int __return_630;
int __return_655;
int __return_645;
int __return_634;
int __return_650;
int __return_649;
int __return_646;
int __return_636;
int __return_640;
int __return_653;
int __return_641;
int __return_648;
int __return_656;
int __return_642;
int __return_644;
int __return_633;
int __return_626;
int __return_654;
int __return_629;
int __return_631;
int __return_627;
int __return_643;
int __return_628;
int __return_632;
int __return_638;
int __return_647;
int __return_637;
int __return_635;
int main()
{
int a;
int b;
int c;
int isoscles = 0;
int scalene=0;
int triangle=0;
int equilateral=0;
int s;
if (a > 0)
{
if (b > 0)
{
if (c > 0)
{
if (a < (b + c))
{
triangle = 1;
if (a >= b)
{
if (b >= a)
{
isoscles = 1;
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
equilateral = 1;
triangle = 1;
s = 0;
 __return_639 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
triangle = 1;
scalene = 1;
s = 0;
 __return_652 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_651 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_630 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
triangle = 1;
label_530:; 
scalene = 1;
s = 0;
 __return_655 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
triangle = 1;
goto label_528;
}
}
else 
{
if ((a + b) > c)
{
triangle = 1;
label_518:; 
scalene = 1;
s = 0;
 __return_645 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_634 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
}
else 
{
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
triangle = 1;
scalene = 1;
s = 0;
 __return_650 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
triangle = 1;
scalene = 1;
s = 0;
 __return_649 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_646 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_636 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
}
else 
{
triangle = -1;
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
triangle = 1;
goto label_530;
}
else 
{
triangle = 1;
label_528:; 
scalene = 1;
s = 0;
 __return_640 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
triangle = 1;
goto label_518;
}
}
}
else 
{
triangle = -1;
if (a >= b)
{
if (b >= a)
{
isoscles = 1;
triangle = 1;
scalene = 1;
s = 0;
 __return_653 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
triangle = 1;
scalene = 1;
s = 0;
 __return_641 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
triangle = 1;
scalene = 1;
s = 0;
 __return_648 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
triangle = -1;
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
triangle = 1;
scalene = 1;
s = 0;
 __return_656 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
triangle = 1;
scalene = 1;
s = 0;
 __return_642 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_644 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_633 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
}
else 
{
triangle = -1;
if (a >= b)
{
if (b >= a)
{
isoscles = 1;
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
equilateral = 1;
s = 0;
 __return_626 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_654 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_629 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
s = 0;
 __return_631 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
else 
{
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
s = 0;
 __return_627 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_643 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_628 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
s = 0;
 __return_632 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
if (b >= c)
{
if (c >= b)
{
isoscles = 1;
s = 0;
 __return_638 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
if ((a + b) > c)
{
triangle = 1;
scalene = 1;
s = 0;
 __return_647 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
else 
{
s = 0;
 __return_637 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
else 
{
s = 0;
 __return_635 = ((equilateral + isoscles) + triangle) + scalene;
return 1;
}
}
}
}
