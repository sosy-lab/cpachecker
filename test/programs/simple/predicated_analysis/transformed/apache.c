typedef int size_t;
extern int nondet_char() ;
extern char *ap_cpystrn(char *dst , char const   *src , size_t dst_size ) ;
int flag  =    0;
char *get_tag(char *tag , int tagbuf_len );
int main(void);
char *__return_1248;
char *__return_1244;
char *__return_1220;
char *__return_1213;
char *__return_1245;
int __return_1252;
char *__return_1157;
char *__return_1222;
char *__return_1216;
char *__return_1204;
char *__return_1109;
char *__return_1207;
char *__return_1188;
char *__return_1224;
char *__return_1218;
char *__return_1211;
char *__return_1228;
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
 __return_1248 = tag;
goto label_1245;
}
else 
{
goto label_1235;
}
}
else 
{
label_1235:; 
 __return_1244 = (char *)((void *)0);
goto label_1245;
}
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
 __return_1220 = (char *)((void *)0);
goto label_1245;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1213 = (char *)((void *)0);
goto label_1245;
}
else 
{
goto label_1045;
}
}
else 
{
label_1045:; 
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
goto label_1134;
}
else 
{
label_1120:; 
goto label_1097;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1245 = tag;
label_1245:; 
}
else 
{
label_1097:; 
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
goto label_1134;
}
else 
{
goto label_1120;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1157 = tag;
goto label_1245;
}
else 
{
goto label_1097;
}
}
}
 __return_1252 = 0;
return 1;
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
 __return_1222 = (char *)((void *)0);
goto label_1245;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1216 = (char *)((void *)0);
goto label_1245;
}
else 
{
goto label_1047;
}
}
else 
{
label_1047:; 
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
 __return_1204 = (char *)((void *)0);
goto label_1245;
}
else 
{
label_1134:; 
goto label_1139;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1109 = tag;
goto label_1245;
}
else 
{
label_1139:; 
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
 __return_1207 = (char *)((void *)0);
goto label_1245;
}
else 
{
goto label_1134;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = 0;
 __return_1188 = tag;
goto label_1245;
}
else 
{
goto label_1139;
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
 __return_1224 = (char *)((void *)0);
goto label_1245;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_1218 = (char *)((void *)0);
goto label_1245;
}
else 
{
goto label_1049;
}
}
else 
{
label_1049:; 
term = c;
tmp___4 = nondet_char();
c = (char)tmp___4;
flag = t;
*(tag + t) = 0;
 __return_1211 = (char *)((void *)0);
goto label_1245;
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
 __return_1228 = (char *)((void *)0);
goto label_1245;
}
}
}
}
}
}
