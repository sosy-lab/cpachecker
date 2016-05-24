extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int t2_pc  =    0;
int m_st  ;
int t1_st  ;
int t2_st  ;
int m_i  ;
int t1_i  ;
int t2_i  ;
int M_E  =    2;
int T1_E  =    2;
int T2_E  =    2;
int E_1  =    2;
int E_2  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
int is_transmit2_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
void transmit2(void);
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
int __return_6572;
int __return_6588;
int __return_6604;
int __return_8340;
int __return_8389;
int __return_8405;
int __return_8421;
int __return_8477;
int __return_8506;
int __return_8522;
int __return_8538;
int __return_8598;
int __return_8611;
int __return_8612;
int __return_8636;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
t2_i = 1;
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
t2_st = 0;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_6542;
}
else 
{
label_6542:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_6572 = __retres1;
}
tmp = __return_6572;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_6588 = __retres1;
}
tmp___0 = __return_6588;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_6604 = __retres1;
}
tmp___1 = __return_6604;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_6617;
}
else 
{
label_6617:; 
if (!(T1_E == 1))
{
label_6626:; 
if (!(T2_E == 1))
{
label_6633:; 
if (!(E_1 == 1))
{
label_6640:; 
if (!(E_2 == 1))
{
label_6647:; 
}
else 
{
E_2 = 2;
goto label_6647;
}
kernel_st = 1;
label_6657:; 
{
int tmp ;
label_7152:; 
{
int __retres1 ;
__retres1 = 1;
 __return_8340 = __retres1;
}
tmp = __return_8340;
}
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_8359;
}
else 
{
label_8359:; 
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_8389 = __retres1;
}
tmp = __return_8389;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_8405 = __retres1;
}
tmp___0 = __return_8405;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_8421 = __retres1;
}
tmp___1 = __return_8421;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_8434;
}
else 
{
label_8434:; 
if (!(T1_E == 1))
{
label_8443:; 
if (!(T2_E == 1))
{
label_8450:; 
if (!(E_1 == 1))
{
label_8457:; 
if (!(E_2 == 1))
{
label_8464:; 
}
else 
{
E_2 = 2;
goto label_8464;
}
{
int __retres1 ;
__retres1 = 1;
 __return_8477 = __retres1;
}
tmp = __return_8477;
if (tmp < 1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_8506 = __retres1;
}
tmp = __return_8506;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_8522 = __retres1;
}
tmp___0 = __return_8522;
t1_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_8538 = __retres1;
}
tmp___1 = __return_8538;
t2_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_8551;
}
else 
{
label_8551:; 
if (!(T1_E == 1))
{
label_8559:; 
if (!(T2_E == 1))
{
label_8566:; 
if (!(E_1 == 1))
{
label_8573:; 
if (!(E_2 == 1))
{
label_8580:; 
}
else 
{
E_2 = 2;
goto label_8580;
}
goto label_8483;
}
else 
{
E_1 = 2;
goto label_8573;
}
}
else 
{
T2_E = 2;
goto label_8566;
}
}
else 
{
T1_E = 2;
goto label_8559;
}
}
}
}
else 
{
label_8483:; 
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_8598 = __retres1;
}
tmp = __return_8598;
if (tmp < 1)
{
__retres2 = 0;
 __return_8611 = __retres2;
}
else 
{
__retres2 = 1;
 __return_8612 = __retres2;
}
tmp___0 = __return_8611;
tmp___0 = __return_8612;
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
goto label_6657;
}
label_8632:; 
__retres1 = 0;
 __return_8636 = __retres1;
return 1;
}
goto label_8632;
}
}
else 
{
E_1 = 2;
goto label_8457;
}
}
else 
{
T2_E = 2;
goto label_8450;
}
}
else 
{
T1_E = 2;
goto label_8443;
}
}
}
}
}
else 
{
E_1 = 2;
goto label_6640;
}
}
else 
{
T2_E = 2;
goto label_6633;
}
}
else 
{
T1_E = 2;
goto label_6626;
}
}
}
}
}
}
