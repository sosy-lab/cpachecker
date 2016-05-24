typedef unsigned int u_int;
typedef unsigned char u_int8_t;
typedef int size_t;
int flag  =    0;
void *memcpy(void *dest , void const   *src , size_t n );
static u_int encode_ie(void *buf , size_t bufsize , u_int8_t const   *ie , size_t ielen ,
                       char const   *leader , size_t leader_len );
int main(void);
u_int __return_263;
int __return_277;
void *__return_173;
u_int __return_257;
int __return_275;
u_int __return_228;
int __return_273;
u_int __return_199;
int main()
{
u_int8_t buf[6] ;
u_int8_t ie[5] ;
char leader[1] ;
{
void *__tmp_1 = (void *)buf;
size_t __tmp_2 = 6;
u_int8_t *__tmp_3 = (u_int8_t *)ie;
size_t __tmp_4 = 5;
const char *__tmp_5 = (const char *)leader;
size_t __tmp_6 = 1;
void *buf = __tmp_1;
size_t bufsize = __tmp_2;
u_int8_t *ie = __tmp_3;
size_t ielen = __tmp_4;
const char *leader = __tmp_5;
size_t leader_len = __tmp_6;
u_int8_t *p ;
int i ;
int index ;
int tmp ;
index = 0;
if (bufsize < leader_len)
{
 __return_263 = (u_int )0;
}
else 
{
p = (u_int8_t *)buf;
{
void *__tmp_7 = (void *)p;
const void *__tmp_8 = (const void *)leader;
size_t __tmp_9 = leader_len;
void *dest = __tmp_7;
const void *src = __tmp_8;
size_t n = __tmp_9;
int i ;
unsigned char *s ;
unsigned char *d ;
s = (unsigned char *)src;
d = (unsigned char *)dest;
i = n - 1;
flag = i;
*(d + i) = *(s + i);
i = i - 1;
 __return_173 = dest;
}
bufsize = bufsize - leader_len;
index = index + leader_len;
i = 0;
if (i < ielen)
{
if (bufsize > 2)
{
flag = index;
*(p + index) = (u_int8_t )'x';
flag = index + 1;
*(p + (index + 1)) = (u_int8_t )'x';
index = index + 2;
bufsize = bufsize - 2;
i = i + 1;
if (i < ielen)
{
if (bufsize > 2)
{
flag = index;
*(p + index) = (u_int8_t )'x';
flag = index + 1;
*(p + (index + 1)) = (u_int8_t )'x';
index = index + 2;
bufsize = bufsize - 2;
i = i + 1;
if (i < ielen)
{
goto label_247;
}
else 
{
label_247:; 
if (i == ielen)
{
tmp = index;
goto label_256;
}
else 
{
tmp = 0;
label_256:; 
 __return_257 = (u_int )tmp;
}
 __return_275 = 0;
return 1;
}
}
else 
{
goto label_218;
}
}
else 
{
label_218:; 
if (i == ielen)
{
tmp = index;
goto label_227;
}
else 
{
tmp = 0;
label_227:; 
 __return_228 = (u_int )tmp;
}
 __return_273 = 0;
return 1;
}
}
else 
{
goto label_189;
}
}
else 
{
label_189:; 
if (i == ielen)
{
tmp = index;
goto label_198;
}
else 
{
tmp = 0;
label_198:; 
 __return_199 = (u_int )tmp;
}
goto label_265;
}
}
label_265:; 
 __return_277 = 0;
return 1;
}
}
