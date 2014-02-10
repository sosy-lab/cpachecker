typedef int size_t;
extern int nondet_char() ;
extern char *ap_cpystrn(char *dst , char const   *src , size_t dst_size ) ;
int flag  =    0;
char *get_tag(char *tag , int tagbuf_len );
int main(void);
char *__return_2180;
char *__return_2369;
int __return_2420;
char *__return_2198;
char *__return_2219;
char *__return_2418;
char *__return_2409;
char *__return_2293;
char *__return_2245;
char *__return_2284;
char *__return_2405;
char *__return_2373;
char *__return_2322;
char *__return_2364;
char *__return_2413;
char *__return_2368;
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
 __return_2180 = tag;
goto label_2369;
}
else 
{
goto label_2163;
}
}
else 
{
label_2163:; 
 __return_2369 = (char *)((void *)0);
label_2369:; 
}
 __return_2420 = 0;
return 1;
}
else 
{
if (((int)c) == 61)
{
flag = t;
*(tag + t) = (char)0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_2198 = (char *)((void *)0);
goto label_2369;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_2219 = (char *)((void *)0);
goto label_2369;
}
else 
{
goto label_2215;
}
}
else 
{
label_2215:; 
term = c;
label_2227:; 
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = (char)'a';
t = t + 1;
label_2313:; 
label_2319:; 
tmp___4 = nondet_char();
c = (char)tmp___4;
if (((int)c) == 92)
{
tmp___5 = nondet_char();
c = (char)tmp___5;
if (((int)c) != ((int)term))
{
flag = t;
*(tag + t) = (char)'a';
t = t + 1;
flag = t;
*(tag + t) = (char)0;
 __return_2418 = (char *)((void *)0);
goto label_2369;
}
else 
{
goto label_2313;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = (char)0;
 __return_2409 = tag;
goto label_2369;
}
else 
{
goto label_2319;
}
}
}
else 
{
goto label_2264;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = (char)0;
 __return_2293 = tag;
goto label_2369;
}
else 
{
label_2264:; 
goto label_2227;
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
*(tag + t) = (char)0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_2245 = (char *)((void *)0);
goto label_2369;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_2284 = (char *)((void *)0);
goto label_2369;
}
else 
{
goto label_2276;
}
}
else 
{
label_2276:; 
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
*(tag + t) = (char)'a';
t = t + 1;
flag = t;
*(tag + t) = (char)0;
 __return_2405 = (char *)((void *)0);
goto label_2369;
}
else 
{
goto label_2313;
}
}
else 
{
if (((int)c) == ((int)term))
{
flag = t;
*(tag + t) = (char)0;
 __return_2373 = tag;
goto label_2369;
}
else 
{
goto label_2319;
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
*(tag + t) = (char)0;
t = t + 1;
tag_val = tag + t;
if (((int)c) != 61)
{
 __return_2322 = (char *)((void *)0);
goto label_2369;
}
else 
{
tmp___3 = nondet_char();
c = (char)tmp___3;
if (((int)c) != 34)
{
if (((int)c) != 39)
{
 __return_2364 = (char *)((void *)0);
goto label_2369;
}
else 
{
goto label_2356;
}
}
else 
{
label_2356:; 
term = c;
tmp___4 = nondet_char();
c = (char)tmp___4;
flag = t;
*(tag + t) = (char)0;
 __return_2413 = (char *)((void *)0);
goto label_2369;
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
*(tag + t) = (char)0;
 __return_2368 = (char *)((void *)0);
goto label_2369;
}
}
}
}
}
}
