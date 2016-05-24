extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int m_st  ;
int t1_st  ;
int m_i  ;
int t1_i  ;
int M_E  =    2;
int T1_E  =    2;
int E_1  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void activate_threads(void);
void fire_time_events(void);
void reset_time_events(void);
void init_model(void);
int stop_simulation(void);
void start_simulation(void);
int main(void);
int __return_7740;
int __return_7756;
int __return_7813;
int __return_7911;
int __return_7927;
int __return_7971;
int __return_8000;
int __return_8016;
int __return_8066;
int __return_8077;
int __return_8078;
int __return_8210;
int __return_8140;
int __return_8096;
int __return_8107;
int __return_8108;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
m_st = 0;
t1_st = 0;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_7718;
}
else 
{
label_7718:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_7740 = __retres1;
}
tmp = __return_7740;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_7756 = __retres1;
}
tmp___0 = __return_7756;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_7776;
}
else 
{
label_7776:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_7782;
}
else 
{
label_7782:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_7788;
}
else 
{
label_7788:; 
}
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_7813 = __retres1;
}
tmp = __return_7813;
}
label_7823:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_7889;
}
else 
{
label_7889:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_7911 = __retres1;
}
tmp = __return_7911;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_7927 = __retres1;
}
tmp___0 = __return_7927;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_7947;
}
else 
{
label_7947:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_7953;
}
else 
{
label_7953:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_7959;
}
else 
{
label_7959:; 
}
{
int __retres1 ;
__retres1 = 1;
 __return_7971 = __retres1;
}
tmp = __return_7971;
if (tmp < 1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_8000 = __retres1;
}
tmp = __return_8000;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_8016 = __retres1;
}
tmp___0 = __return_8016;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_8036;
}
else 
{
label_8036:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_8042;
}
else 
{
label_8042:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_8048;
}
else 
{
label_8048:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_8066 = __retres1;
}
tmp = __return_8066;
if (tmp < 1)
{
__retres2 = 0;
 __return_8077 = __retres2;
}
else 
{
__retres2 = 1;
 __return_8078 = __retres2;
}
tmp___0 = __return_8077;
label_8082:; 
tmp___0 = __return_8078;
label_8080:; 
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_8140 = __retres1;
}
tmp = __return_8140;
}
goto label_7823;
}
label_8205:; 
__retres1 = 0;
 __return_8210 = __retres1;
return 1;
}
goto label_8205;
}
}
}
}
else 
{
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_8096 = __retres1;
}
tmp = __return_8096;
if (tmp < 1)
{
__retres2 = 0;
 __return_8107 = __retres2;
}
else 
{
__retres2 = 1;
 __return_8108 = __retres2;
}
tmp___0 = __return_8107;
goto label_8082;
tmp___0 = __return_8108;
goto label_8080;
}
}
}
}
}
}
}
}
}
}
}
}
