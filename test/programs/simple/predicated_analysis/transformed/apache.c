typedef int size_t;
extern int nondet_char() ;
extern char *ap_cpystrn(char *dst , char const   *src , size_t dst_size ) ;
int flag  =    0;
char *get_tag(char *tag , int tagbuf_len );
int main(void);
char *__return_1537;
char *__return_1751;
int __return_1806;
char *__return_1555;
char *__return_1576;
char *__return_1804;
char *__return_1795;
char *__return_1651;
char *__return_1730;
char *__return_1602;
char *__return_1642;
char *__return_1791;
char *__return_1756;
char *__return_1689;
char *__return_1746;
char *__return_1799;
char *__return_1750;
int main()
{
char tag[4] ;
{
char *__tmp_1 = tag;
int __tmp_2 = 4;
char *tag = __tmp_1;
int tagbuf_len = __tmp_2;
char *tag_val ;
char c ;
char term ;
int t ;
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
int tmp___4 ;
int tmp___5 ;
t = 0;
tagbuf_len = tagbuf_len - 1;
tmp = nondet_char();
c = (char)tmp;
if (((int)c) == 45)
{
tmp___0 = nondet_char();
c = (char)tmp___0;
if (((int)c) == 45)
{
tmp___1 = nondet_char();
c = (char)tmp___1;
if (((int)c) == 62)
{
ap_cpystrn(tag, "done", tagbuf_len);
 __return_1537 = tag;
goto label_1751;
}
else 
{
goto label_1520;
}
}
else 
{
label_1520:; 
 __return_1751 = (char *)((void *)0);
label_1751:; 
}
 __return_1806 = 0;
return 1;
}
else 
{
if (((int)c) == 61)
{
flag = t;
*(tag + t) = 0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_1555 = (char *)((void *)0);
goto label_1751;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1576 = (char *)((void *)0);
goto label_1751;
}
else 
{
goto label_1572;
}
}
else 
{
label_1572:; 
term = c;
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = 92;
t = t + 1;
label_1675:; 
label_1683:; 
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = 92;
t = t + 1;
flag = t;
*(tag + t) = 0;
 __return_1804 = (char *)((void *)0);
goto label_1751;
}
else 
{
goto label_1675;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1795 = tag;
goto label_1751;
}
else 
{
goto label_1683;
}
}
}
else 
{
label_1638:; 
goto label_1621;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1651 = tag;
goto label_1751;
}
else 
{
label_1621:; 
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = 92;
t = t + 1;
goto label_1675;
}
else 
{
goto label_1638;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1730 = tag;
goto label_1751;
}
else 
{
goto label_1621;
}
}
}
}
}
}
}
else 
{
flag = t;
*(tag + t) = c;
t = t + 1;
tmp___2 = nondet_char();
c = (char)tmp___2;
if (((int)c) == 61)
{
flag = t;
*(tag + t) = 0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_1602 = (char *)((void *)0);
goto label_1751;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1642 = (char *)((void *)0);
goto label_1751;
}
else 
{
goto label_1633;
}
}
else 
{
label_1633:; 
term = c;
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = 92;
t = t + 1;
flag = t;
*(tag + t) = 0;
 __return_1791 = (char *)((void *)0);
goto label_1751;
}
else 
{
goto label_1675;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1756 = tag;
goto label_1751;
}
else 
{
goto label_1683;
}
}
}
}
}
else 
{
flag = t;
*(tag + t) = c;
t = t + 1;
tmp___2 = nondet_char();
c = (char)tmp___2;
if (((int)c) == 61)
{
flag = t;
*(tag + t) = 0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_1689 = (char *)((void *)0);
goto label_1751;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1746 = (char *)((void *)0);
goto label_1751;
}
else 
{
goto label_1737;
}
}
else 
{
label_1737:; 
term = c;
tmp___4 = nondet_char();
c = (char)tmp___4;
flag = t;
*(tag + t) = 0;
 __return_1799 = (char *)((void *)0);
goto label_1751;
}
}
}
else 
{
flag = t;
*(tag + t) = c;
t = t + 1;
tmp___2 = nondet_char();
c = (char)tmp___2;
flag = t;
*(tag + t) = 0;
 __return_1750 = (char *)((void *)0);
goto label_1751;
}
}
}
}
}
}
