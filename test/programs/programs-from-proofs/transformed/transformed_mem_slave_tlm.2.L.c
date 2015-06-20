extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_run_st  ;
int m_run_i  ;
int m_run_pc  ;
int s_memory0  ;
int s_memory1  ;
int s_run_st  ;
int s_run_i  ;
int s_run_pc  ;
int c_m_lock  ;
int c_m_ev  ;
int c_req_type  ;
int c_req_a  ;
int c_req_d  ;
int c_rsp_type  ;
int c_rsp_status  ;
int c_rsp_d  ;
int c_empty_req  ;
int c_empty_rsp  ;
int c_read_req_ev  ;
int c_write_req_ev  ;
int c_read_rsp_ev  ;
int c_write_rsp_ev  ;
static int d_t  ;
static int a_t  ;
static int req_t_type  ;
static int req_t_a  ;
static int req_t_d  ;
static int rsp_t_type  ;
static int rsp_t_status  ;
static int rsp_t_d  ;
static int req_tt_type  ;
static int req_tt_a  ;
static int req_tt_d  ;
static int rsp_tt_type  ;
static int rsp_tt_status  ;
static int rsp_tt_d  ;
int s_memory_read(int i);
void s_memory_write(int i, int v);
void m_run(void);
static int req_t_type___0  ;
static int req_t_a___0  ;
static int req_t_d___0  ;
static int rsp_t_type___0  ;
static int rsp_t_status___0  ;
static int rsp_t_d___0  ;
void s_run(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_98374;
int __return_101268;
int __return_102348;
int __return_113510;
int __return_99544;
int __return_103556;
int __return_104646;
int __return_113508;
int __return_107038;
int __return_106268;
int __return_108222;
int __return_109496;
int __return_110844;
int __return_113512;
int __return_113500;
int __return_94536;
int __return_96864;
int __return_113506;
int __return_113504;
int __return_112306;
int __return_113502;
int main()
{
int __retres1 ;
c_m_lock = 0;
c_m_ev = 2;
m_run_i = 1;
m_run_pc = 0;
s_run_i = 1;
s_run_pc = 0;
c_empty_req = 1;
c_empty_rsp = 1;
c_read_req_ev = 2;
c_write_req_ev = 2;
c_read_rsp_ev = 2;
c_write_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 2;
{
int kernel_st ;
kernel_st = 0;
m_run_st = 0;
s_run_st = 0;
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_92228:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92462;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_92832:; 
label_92834:; 
label_92856:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92834;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_92832;
}
}
else 
{
goto label_92856;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93004;
}
else 
{
label_93004:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_93074:; 
label_93086:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_93104;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93302;
}
else 
{
label_93302:; 
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_1 = req_a;
int __tmp_2 = req_d;
int i = __tmp_1;
int v = __tmp_2;
if (i == 0)
{
s_memory0 = v;
goto label_93342;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_93342;
}
else 
{
{
__VERIFIER_error();
}
label_93342:; 
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93410;
}
else 
{
label_93410:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_92772;
}
}
}
}
}
}
else 
{
label_93104:; 
if (((int)s_run_st) == 0)
{
goto label_93086;
}
else 
{
}
label_93556:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96046;
}
else 
{
label_96046:; 
if (((int)s_run_st) == 0)
{
goto label_103300;
}
else 
{
}
__retres1 = 0;
goto label_113482;
}
}
}
label_93088:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92838;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_92550;
}
}
else 
{
goto label_92852;
}
}
}
}
}
else 
{
label_92462:; 
goto label_92228;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_92448:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92466;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_92550:; 
label_92838:; 
label_92852:; 
if (((int)s_run_st) == 0)
{
goto label_93088;
}
else 
{
}
label_95582:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96036;
}
else 
{
label_96036:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_97784:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_98552;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_98552:; 
goto label_98072;
}
}
else 
{
label_98072:; 
goto label_97784;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_97952;
}
else 
{
label_97952:; 
c_write_req_ev = 2;
m_run_st = 2;
goto label_97992;
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_97954;
}
else 
{
label_97954:; 
c_write_req_ev = 2;
m_run_st = 2;
label_97992:; 
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_98044:; 
label_98056:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_98074;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_98272;
}
else 
{
label_98272:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_3 = req_a;
int i = __tmp_3;
int x;
if (i == 0)
{
x = s_memory0;
goto label_98364;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_98364;
}
else 
{
{
__VERIFIER_error();
}
label_98364:; 
 __return_98374 = x;
}
rsp_d = __return_98374;
rsp_status = 1;
goto label_98294;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_4 = req_a;
int __tmp_5 = req_d;
int i = __tmp_4;
int v = __tmp_5;
if (i == 0)
{
s_memory0 = v;
goto label_98320;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_98320;
}
else 
{
{
__VERIFIER_error();
}
label_98320:; 
}
rsp_status = 1;
goto label_98294;
}
}
}
else 
{
rsp_status = 0;
label_98294:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_98432;
}
else 
{
label_98432:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_98478:; 
label_98554:; 
label_98568:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_98554;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_98478;
}
}
else 
{
goto label_98568;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_98710;
}
else 
{
label_98710:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_98724;
}
else 
{
label_98724:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_98780;
}
else 
{
label_98780:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_99044;
}
else 
{
label_99044:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
goto label_98044;
}
label_99140:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_99738;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_99738:; 
goto label_99164;
}
}
else 
{
label_99164:; 
label_100396:; 
if (((int)s_run_st) == 0)
{
goto label_99140;
}
else 
{
}
label_100414:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100602;
}
else 
{
label_100602:; 
label_100662:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_100678:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_101446;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_101446:; 
goto label_100966;
}
}
else 
{
label_100966:; 
goto label_100678;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100846;
}
else 
{
label_100846:; 
c_write_req_ev = 2;
m_run_st = 2;
goto label_100886;
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100848;
}
else 
{
label_100848:; 
c_write_req_ev = 2;
m_run_st = 2;
label_100886:; 
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_100938:; 
label_100950:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_100968;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101166;
}
else 
{
label_101166:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_6 = req_a;
int i = __tmp_6;
int x;
if (i == 0)
{
x = s_memory0;
goto label_101258;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_101258;
}
else 
{
{
__VERIFIER_error();
}
label_101258:; 
 __return_101268 = x;
}
rsp_d = __return_101268;
rsp_status = 1;
goto label_101188;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_7 = req_a;
int __tmp_8 = req_d;
int i = __tmp_7;
int v = __tmp_8;
if (i == 0)
{
s_memory0 = v;
goto label_101214;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_101214;
}
else 
{
{
__VERIFIER_error();
}
label_101214:; 
}
rsp_status = 1;
goto label_101188;
}
}
}
else 
{
rsp_status = 0;
label_101188:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101326;
}
else 
{
label_101326:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_101372:; 
label_101448:; 
label_101462:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_101448;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_101372;
}
}
else 
{
goto label_101462;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101604;
}
else 
{
label_101604:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_101618;
}
else 
{
label_101618:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101674;
}
else 
{
label_101674:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101938;
}
else 
{
label_101938:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
goto label_100938;
}
goto label_100940;
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_101790;
}
else 
{
label_101790:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_102012:; 
label_102028:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_102046;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_102246;
}
else 
{
label_102246:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_9 = req_a;
int i = __tmp_9;
int x;
if (i == 0)
{
x = s_memory0;
goto label_102338;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_102338;
}
else 
{
{
__VERIFIER_error();
}
label_102338:; 
 __return_102348 = x;
}
rsp_d = __return_102348;
rsp_status = 1;
goto label_102268;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_10 = req_a;
int __tmp_11 = req_d;
int i = __tmp_10;
int v = __tmp_11;
if (i == 0)
{
s_memory0 = v;
goto label_102294;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_102294;
}
else 
{
{
__VERIFIER_error();
}
label_102294:; 
}
rsp_status = 1;
goto label_102268;
}
}
}
else 
{
rsp_status = 0;
label_102268:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_102418;
}
else 
{
label_102418:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_102464:; 
label_102538:; 
label_102552:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_102538;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_102464;
}
}
else 
{
goto label_102552;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_102700;
}
else 
{
label_102700:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_102714;
}
else 
{
label_102714:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_102772;
}
else 
{
label_102772:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_102798;
}
else 
{
{
__VERIFIER_error();
}
label_102798:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_102902;
}
else 
{
label_102902:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_102012;
}
goto label_102014;
}
else 
{
}
label_102986:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_103160;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_103160:; 
goto label_102998;
}
}
else 
{
label_102998:; 
if (((int)s_run_st) == 0)
{
goto label_102986;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_102046:; 
if (((int)s_run_st) == 0)
{
goto label_102028;
}
else 
{
}
goto label_95556;
}
}
label_102014:; 
label_102030:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_102540;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_102540:; 
goto label_102048;
}
}
else 
{
label_102048:; 
if (((int)s_run_st) == 0)
{
goto label_102030;
}
else 
{
}
goto label_95476;
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
}
else 
{
label_100968:; 
if (((int)s_run_st) == 0)
{
goto label_100950;
}
else 
{
}
goto label_100494;
}
}
}
label_100940:; 
label_100952:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_101450;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_101450:; 
goto label_100970;
}
}
else 
{
label_100970:; 
if (((int)s_run_st) == 0)
{
goto label_100952;
}
else 
{
}
goto label_100414;
}
}
}
}
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100606;
}
else 
{
label_100606:; 
if (((int)s_run_st) == 0)
{
label_105578:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_105594:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_105706;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_105706:; 
goto label_105624;
}
}
else 
{
label_105624:; 
goto label_105594;
}
}
else 
{
}
goto label_100414;
}
}
else 
{
}
__retres1 = 0;
label_113480:; 
 __return_113510 = __retres1;
return 1;
}
}
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_98896;
}
else 
{
label_98896:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_99118:; 
label_99136:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_99160;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_99442;
}
else 
{
label_99442:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_12 = req_a;
int i = __tmp_12;
int x;
if (i == 0)
{
x = s_memory0;
goto label_99534;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_99534;
}
else 
{
{
__VERIFIER_error();
}
label_99534:; 
 __return_99544 = x;
}
rsp_d = __return_99544;
rsp_status = 1;
goto label_99464;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_13 = req_a;
int __tmp_14 = req_d;
int i = __tmp_13;
int v = __tmp_14;
if (i == 0)
{
s_memory0 = v;
goto label_99490;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_99490;
}
else 
{
{
__VERIFIER_error();
}
label_99490:; 
}
rsp_status = 1;
goto label_99464;
}
}
}
else 
{
rsp_status = 0;
label_99464:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_99614;
}
else 
{
label_99614:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_99660:; 
label_99734:; 
label_99754:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_99734;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_99660;
}
}
else 
{
goto label_99754;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_99902;
}
else 
{
label_99902:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_99916;
}
else 
{
label_99916:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_99974;
}
else 
{
label_99974:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_100000;
}
else 
{
{
__VERIFIER_error();
}
label_100000:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100104;
}
else 
{
label_100104:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_99118;
}
goto label_99120;
}
else 
{
}
label_100188:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_100362;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_100362:; 
goto label_100200;
}
}
else 
{
label_100200:; 
if (((int)s_run_st) == 0)
{
goto label_100188;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_99160:; 
if (((int)s_run_st) == 0)
{
goto label_99136;
}
else 
{
}
goto label_95556;
}
}
label_99120:; 
label_99138:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_99736;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_99736:; 
goto label_99162;
}
}
else 
{
label_99162:; 
if (((int)s_run_st) == 0)
{
goto label_99138;
}
else 
{
}
goto label_95476;
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
}
else 
{
label_98074:; 
if (((int)s_run_st) == 0)
{
goto label_98056;
}
else 
{
}
label_100494:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_100604;
}
else 
{
label_100604:; 
if (((int)s_run_st) == 0)
{
label_103300:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_103316:; 
if (((int)s_run_st) == 0)
{
label_103340:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_103346;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_103454;
}
else 
{
label_103454:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_15 = req_a;
int i = __tmp_15;
int x;
if (i == 0)
{
x = s_memory0;
goto label_103546;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_103546;
}
else 
{
{
__VERIFIER_error();
}
label_103546:; 
 __return_103556 = x;
}
rsp_d = __return_103556;
rsp_status = 1;
goto label_103476;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_16 = req_a;
int __tmp_17 = req_d;
int i = __tmp_16;
int v = __tmp_17;
if (i == 0)
{
s_memory0 = v;
goto label_103502;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_103502;
}
else 
{
{
__VERIFIER_error();
}
label_103502:; 
}
rsp_status = 1;
goto label_103476;
}
}
}
else 
{
rsp_status = 0;
label_103476:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_103614;
}
else 
{
label_103614:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_103660:; 
label_103662:; 
label_103668:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_103662;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_103660;
}
}
else 
{
goto label_103668;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_103808;
}
else 
{
label_103808:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_103822;
}
else 
{
label_103822:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_103878;
}
else 
{
label_103878:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_104142;
}
else 
{
label_104142:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
goto label_103340;
}
label_104242:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_104840;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_104840:; 
goto label_104266;
}
}
else 
{
label_104266:; 
if (((int)s_run_st) == 0)
{
goto label_104242;
}
else 
{
}
goto label_100414;
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_103994;
}
else 
{
label_103994:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_104216:; 
label_104236:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_104262;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_104544;
}
else 
{
label_104544:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_18 = req_a;
int i = __tmp_18;
int x;
if (i == 0)
{
x = s_memory0;
goto label_104636;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_104636;
}
else 
{
{
__VERIFIER_error();
}
label_104636:; 
 __return_104646 = x;
}
rsp_d = __return_104646;
rsp_status = 1;
goto label_104566;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_19 = req_a;
int __tmp_20 = req_d;
int i = __tmp_19;
int v = __tmp_20;
if (i == 0)
{
s_memory0 = v;
goto label_104592;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_104592;
}
else 
{
{
__VERIFIER_error();
}
label_104592:; 
}
rsp_status = 1;
goto label_104566;
}
}
}
else 
{
rsp_status = 0;
label_104566:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_104716;
}
else 
{
label_104716:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_104762:; 
label_104836:; 
label_104856:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_104836;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_104762;
}
}
else 
{
goto label_104856;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_105004;
}
else 
{
label_105004:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_105018;
}
else 
{
label_105018:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_105076;
}
else 
{
label_105076:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_105102;
}
else 
{
{
__VERIFIER_error();
}
label_105102:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_105206;
}
else 
{
label_105206:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_104216;
}
goto label_104218;
}
else 
{
}
label_105290:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_105464;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_105464:; 
goto label_105302;
}
}
else 
{
label_105302:; 
if (((int)s_run_st) == 0)
{
goto label_105290;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_104262:; 
if (((int)s_run_st) == 0)
{
goto label_104236;
}
else 
{
}
goto label_95556;
}
}
label_104218:; 
label_104238:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_104838;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_104838:; 
goto label_104264;
}
}
else 
{
label_104264:; 
if (((int)s_run_st) == 0)
{
goto label_104238;
}
else 
{
}
goto label_95476;
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
}
else 
{
label_103346:; 
goto label_103316;
}
}
else 
{
}
goto label_100494;
}
}
else 
{
}
__retres1 = 0;
label_113482:; 
 __return_113508 = __retres1;
return 1;
}
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_98556;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_98556:; 
goto label_98076;
}
}
else 
{
label_98076:; 
goto label_100396;
}
}
}
}
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96050;
}
else 
{
label_96050:; 
if (((int)s_run_st) == 0)
{
goto label_105578;
}
else 
{
}
__retres1 = 0;
goto label_113480;
}
}
}
}
else 
{
label_92466:; 
if (((int)s_run_st) == 0)
{
goto label_92448;
}
else 
{
}
label_95608:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_105732:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106000;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_106422:; 
label_106424:; 
label_106446:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106424;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_106422;
}
}
else 
{
goto label_106446;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_106616;
}
else 
{
label_106616:; 
c_write_req_ev = 2;
m_run_st = 2;
goto label_106656;
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_106618;
}
else 
{
label_106618:; 
c_write_req_ev = 2;
m_run_st = 2;
label_106656:; 
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_106708:; 
label_106720:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106738;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_106936;
}
else 
{
label_106936:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_21 = req_a;
int i = __tmp_21;
int x;
if (i == 0)
{
x = s_memory0;
goto label_107028;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_107028;
}
else 
{
{
__VERIFIER_error();
}
label_107028:; 
 __return_107038 = x;
}
rsp_d = __return_107038;
rsp_status = 1;
goto label_106958;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_22 = req_a;
int __tmp_23 = req_d;
int i = __tmp_22;
int v = __tmp_23;
if (i == 0)
{
s_memory0 = v;
goto label_106984;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_106984;
}
else 
{
{
__VERIFIER_error();
}
label_106984:; 
}
rsp_status = 1;
goto label_106958;
}
}
}
else 
{
rsp_status = 0;
label_106958:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_107096;
}
else 
{
label_107096:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_106362;
}
}
}
}
}
}
else 
{
label_106738:; 
if (((int)s_run_st) == 0)
{
goto label_106720;
}
else 
{
}
goto label_100494;
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106428;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_106088;
}
}
else 
{
goto label_106442;
}
}
}
}
}
else 
{
label_106000:; 
goto label_105732;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
c_write_req_ev = 2;
m_run_st = 2;
label_105920:; 
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_write_req_ev = 2;
m_run_st = 2;
goto label_105920;
}
label_105984:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106002;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_24 = req_a;
int i = __tmp_24;
int x;
if (i == 0)
{
x = s_memory0;
goto label_106258;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_106258;
}
else 
{
{
__VERIFIER_error();
}
label_106258:; 
 __return_106268 = x;
}
rsp_d = __return_106268;
rsp_status = 1;
goto label_106188;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_25 = req_a;
int __tmp_26 = req_d;
int i = __tmp_25;
int v = __tmp_26;
if (i == 0)
{
s_memory0 = v;
goto label_106214;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_106214;
}
else 
{
{
__VERIFIER_error();
}
label_106214:; 
}
rsp_status = 1;
goto label_106188;
}
}
}
else 
{
rsp_status = 0;
label_106188:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_106362:; 
label_106426:; 
label_106444:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106426;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_106362;
}
}
else 
{
goto label_106444;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_107388;
}
else 
{
label_107388:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_107402;
}
else 
{
label_107402:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_107458;
}
else 
{
label_107458:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_107722;
}
else 
{
label_107722:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
goto label_106708;
}
label_107818:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_108416;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_108416:; 
goto label_107842;
}
}
else 
{
label_107842:; 
label_109074:; 
if (((int)s_run_st) == 0)
{
goto label_107818;
}
else 
{
}
goto label_100414;
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_107574;
}
else 
{
label_107574:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_107796:; 
label_107814:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_107838;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_108120;
}
else 
{
label_108120:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_27 = req_a;
int i = __tmp_27;
int x;
if (i == 0)
{
x = s_memory0;
goto label_108212;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_108212;
}
else 
{
{
__VERIFIER_error();
}
label_108212:; 
 __return_108222 = x;
}
rsp_d = __return_108222;
rsp_status = 1;
goto label_108142;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_28 = req_a;
int __tmp_29 = req_d;
int i = __tmp_28;
int v = __tmp_29;
if (i == 0)
{
s_memory0 = v;
goto label_108168;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_108168;
}
else 
{
{
__VERIFIER_error();
}
label_108168:; 
}
rsp_status = 1;
goto label_108142;
}
}
}
else 
{
rsp_status = 0;
label_108142:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_108292;
}
else 
{
label_108292:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_108338:; 
label_108412:; 
label_108432:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_108412;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_108338;
}
}
else 
{
goto label_108432;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_108580;
}
else 
{
label_108580:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_108594;
}
else 
{
label_108594:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_108652;
}
else 
{
label_108652:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_108678;
}
else 
{
{
__VERIFIER_error();
}
label_108678:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_108782;
}
else 
{
label_108782:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_107796;
}
goto label_107798;
}
else 
{
}
label_108866:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_109040;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_109040:; 
goto label_108878;
}
}
else 
{
label_108878:; 
if (((int)s_run_st) == 0)
{
goto label_108866;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_107838:; 
if (((int)s_run_st) == 0)
{
goto label_107814;
}
else 
{
}
goto label_95556;
}
}
label_107798:; 
label_107816:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_108414;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_108414:; 
goto label_107840;
}
}
else 
{
label_107840:; 
if (((int)s_run_st) == 0)
{
goto label_107816;
}
else 
{
}
goto label_95476;
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
else 
{
label_106002:; 
if (((int)s_run_st) == 0)
{
goto label_105984;
}
else 
{
}
label_109198:; 
kernel_st = 2;
kernel_st = 3;
if (((int)s_run_st) == 0)
{
label_109262:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_109278:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_109308;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_30 = req_a;
int i = __tmp_30;
int x;
if (i == 0)
{
x = s_memory0;
goto label_109486;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_109486;
}
else 
{
{
__VERIFIER_error();
}
label_109486:; 
 __return_109496 = x;
}
rsp_d = __return_109496;
rsp_status = 1;
goto label_109416;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_31 = req_a;
int __tmp_32 = req_d;
int i = __tmp_31;
int v = __tmp_32;
if (i == 0)
{
s_memory0 = v;
goto label_109442;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_109442;
}
else 
{
{
__VERIFIER_error();
}
label_109442:; 
}
rsp_status = 1;
goto label_109416;
}
}
}
else 
{
rsp_status = 0;
label_109416:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_109590:; 
label_109592:; 
label_109598:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_109592;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_109590;
}
}
else 
{
goto label_109598;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_109738;
}
else 
{
label_109738:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_109752;
}
else 
{
label_109752:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_109808;
}
else 
{
label_109808:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_110072;
}
else 
{
label_110072:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_110170:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_110200;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_110414;
}
else 
{
label_110414:; 
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_33 = req_a;
int __tmp_34 = req_d;
int i = __tmp_33;
int v = __tmp_34;
if (i == 0)
{
s_memory0 = v;
goto label_110454;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_110454;
}
else 
{
{
__VERIFIER_error();
}
label_110454:; 
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_110522;
}
else 
{
label_110522:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_109590;
}
}
}
}
}
}
else 
{
label_110200:; 
if (((int)s_run_st) == 0)
{
goto label_110170;
}
else 
{
}
goto label_93556;
}
}
label_110172:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111038;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_111038:; 
goto label_110202;
}
}
else 
{
label_110202:; 
if (((int)s_run_st) == 0)
{
goto label_110172;
}
else 
{
}
goto label_100414;
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_109924;
}
else 
{
label_109924:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_110146:; 
label_110166:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_110196;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_110742;
}
else 
{
label_110742:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_35 = req_a;
int i = __tmp_35;
int x;
if (i == 0)
{
x = s_memory0;
goto label_110834;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_110834;
}
else 
{
{
__VERIFIER_error();
}
label_110834:; 
 __return_110844 = x;
}
rsp_d = __return_110844;
rsp_status = 1;
goto label_110764;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_36 = req_a;
int __tmp_37 = req_d;
int i = __tmp_36;
int v = __tmp_37;
if (i == 0)
{
s_memory0 = v;
goto label_110790;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_110790;
}
else 
{
{
__VERIFIER_error();
}
label_110790:; 
}
rsp_status = 1;
goto label_110764;
}
}
}
else 
{
rsp_status = 0;
label_110764:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_110914;
}
else 
{
label_110914:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_110960:; 
label_111034:; 
label_111058:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111034;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_110960;
}
}
else 
{
goto label_111058;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_111206;
}
else 
{
label_111206:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_111220;
}
else 
{
label_111220:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_111278;
}
else 
{
label_111278:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_111304;
}
else 
{
{
__VERIFIER_error();
}
label_111304:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_111408;
}
else 
{
label_111408:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_110146;
}
goto label_110148;
}
else 
{
}
label_111492:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111666;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_111666:; 
goto label_111504;
}
}
else 
{
label_111504:; 
if (((int)s_run_st) == 0)
{
goto label_111492;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_110196:; 
if (((int)s_run_st) == 0)
{
goto label_110166;
}
else 
{
}
goto label_95556;
}
}
label_110148:; 
label_110168:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111036;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_111036:; 
goto label_110198;
}
}
else 
{
label_110198:; 
if (((int)s_run_st) == 0)
{
goto label_110168;
}
else 
{
}
goto label_95476;
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
else 
{
label_109308:; 
goto label_109278;
}
}
else 
{
}
goto label_109198;
}
}
else 
{
}
__retres1 = 0;
label_113478:; 
 __return_113512 = __retres1;
return 1;
}
}
label_105986:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_106004;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_106088:; 
label_106428:; 
label_106442:; 
goto label_109074;
}
}
else 
{
label_106004:; 
if (((int)s_run_st) == 0)
{
goto label_105986;
}
else 
{
}
goto label_95608;
}
}
}
}
}
else 
{
if (((int)s_run_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_111822:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111852;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_111920:; 
label_111922:; 
label_111928:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_111922;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_111920;
}
}
else 
{
goto label_111928;
}
}
else 
{
}
goto label_95582;
}
}
else 
{
label_111852:; 
goto label_111822;
}
}
else 
{
}
goto label_95608;
}
}
else 
{
}
__retres1 = 0;
 __return_113500 = __retres1;
return 1;
}
}
label_92446:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92464;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_38 = req_a;
int __tmp_39 = req_d;
int i = __tmp_38;
int v = __tmp_39;
if (i == 0)
{
s_memory0 = v;
goto label_92668;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_92668;
}
else 
{
{
__VERIFIER_error();
}
label_92668:; 
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_92772:; 
label_92836:; 
label_92854:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_92836;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_92772;
}
}
else 
{
goto label_92854;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93702;
}
else 
{
label_93702:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_93716;
}
else 
{
label_93716:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93772;
}
else 
{
label_93772:; 
c_m_ev = 2;
a = a + 1;
if (a < 2)
{
req_type = 1;
req_a = a;
req_d = a + 50;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 1;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_94036;
}
else 
{
label_94036:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
goto label_93074;
}
label_94132:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_94730;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_94730:; 
goto label_94156;
}
}
else 
{
label_94156:; 
if (((int)s_run_st) == 0)
{
goto label_94132;
}
else 
{
}
kernel_st = 2;
kernel_st = 3;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96034;
}
else 
{
label_96034:; 
goto label_100662;
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96048;
}
else 
{
label_96048:; 
if (((int)s_run_st) == 0)
{
goto label_105578;
}
else 
{
}
__retres1 = 0;
goto label_113480;
}
}
}
}
else 
{
a = 0;
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_93888;
}
else 
{
label_93888:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_94110:; 
label_94128:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_94152;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_94434;
}
else 
{
label_94434:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_40 = req_a;
int i = __tmp_40;
int x;
if (i == 0)
{
x = s_memory0;
goto label_94526;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_94526;
}
else 
{
{
__VERIFIER_error();
}
label_94526:; 
 __return_94536 = x;
}
rsp_d = __return_94536;
rsp_status = 1;
goto label_94456;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_41 = req_a;
int __tmp_42 = req_d;
int i = __tmp_41;
int v = __tmp_42;
if (i == 0)
{
s_memory0 = v;
goto label_94482;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_94482;
}
else 
{
{
__VERIFIER_error();
}
label_94482:; 
}
rsp_status = 1;
goto label_94456;
}
}
}
else 
{
rsp_status = 0;
label_94456:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_94606;
}
else 
{
label_94606:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_94652:; 
label_94726:; 
label_94746:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_94726;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_94652;
}
}
else 
{
goto label_94746;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_94894;
}
else 
{
label_94894:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_94908;
}
else 
{
label_94908:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_94966;
}
else 
{
label_94966:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_94992;
}
else 
{
{
__VERIFIER_error();
}
label_94992:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_95096;
}
else 
{
label_95096:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_94110;
}
label_95184:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_95446;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_95446:; 
goto label_95202;
}
}
else 
{
label_95202:; 
label_95458:; 
if (((int)s_run_st) == 0)
{
goto label_95184;
}
else 
{
}
label_95476:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96038;
}
else 
{
label_96038:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_96266:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_97054;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_97054:; 
goto label_96560;
}
}
else 
{
label_96560:; 
goto label_96266;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_m_ev) == 1)
{
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96440;
}
else 
{
label_96440:; 
c_write_req_ev = 2;
m_run_st = 2;
goto label_96480;
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96442;
}
else 
{
label_96442:; 
c_write_req_ev = 2;
m_run_st = 2;
label_96480:; 
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_96532:; 
label_96544:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_96562;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96762;
}
else 
{
label_96762:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_43 = req_a;
int i = __tmp_43;
int x;
if (i == 0)
{
x = s_memory0;
goto label_96854;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_96854;
}
else 
{
{
__VERIFIER_error();
}
label_96854:; 
 __return_96864 = x;
}
rsp_d = __return_96864;
rsp_status = 1;
goto label_96784;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_44 = req_a;
int __tmp_45 = req_d;
int i = __tmp_44;
int v = __tmp_45;
if (i == 0)
{
s_memory0 = v;
goto label_96810;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_96810;
}
else 
{
{
__VERIFIER_error();
}
label_96810:; 
}
rsp_status = 1;
goto label_96784;
}
}
}
else 
{
rsp_status = 0;
label_96784:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96934;
}
else 
{
label_96934:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_96980:; 
label_97056:; 
label_97070:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_97056;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_96980;
}
}
else 
{
goto label_97070;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_97220;
}
else 
{
label_97220:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_97234;
}
else 
{
label_97234:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_97292;
}
else 
{
label_97292:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_97318;
}
else 
{
{
__VERIFIER_error();
}
label_97318:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_97422;
}
else 
{
label_97422:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_96532;
}
goto label_96534;
}
else 
{
}
label_97506:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_97680;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_97680:; 
goto label_97518;
}
}
else 
{
label_97518:; 
if (((int)s_run_st) == 0)
{
goto label_97506;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_96562:; 
if (((int)s_run_st) == 0)
{
goto label_96544;
}
else 
{
}
goto label_95556;
}
}
}
label_96534:; 
label_96546:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_97058;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_97058:; 
goto label_96564;
}
}
else 
{
label_96564:; 
if (((int)s_run_st) == 0)
{
goto label_96546;
}
else 
{
}
goto label_95476;
}
}
}
}
}
}
else 
{
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96044;
}
else 
{
label_96044:; 
if (((int)s_run_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_113352:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_113464;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_113464:; 
goto label_113382;
}
}
else 
{
label_113382:; 
goto label_113352;
}
}
else 
{
}
goto label_95476;
}
}
else 
{
}
__retres1 = 0;
 __return_113506 = __retres1;
return 1;
}
}
}
}
else 
{
}
label_95182:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_95444;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_95444:; 
goto label_95200;
}
}
else 
{
label_95200:; 
if (((int)s_run_st) == 0)
{
goto label_95182;
}
else 
{
}
label_95502:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96042;
}
else 
{
label_96042:; 
if (((int)s_run_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_113216:; 
if (((int)s_run_st) == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_113328;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_113328:; 
goto label_113246;
}
}
else 
{
label_113246:; 
goto label_113216;
}
}
else 
{
}
goto label_95502;
}
}
else 
{
}
__retres1 = 0;
 __return_113504 = __retres1;
return 1;
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
}
}
else 
{
label_94152:; 
if (((int)s_run_st) == 0)
{
goto label_94128;
}
else 
{
}
label_95556:; 
kernel_st = 2;
kernel_st = 3;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_96040;
}
else 
{
label_96040:; 
if (((int)s_run_st) == 0)
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_112064:; 
if (((int)s_run_st) == 0)
{
label_112088:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_112094;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_112204;
}
else 
{
label_112204:; 
c_read_req_ev = 2;
rsp_type = req_type;
if (((int)req_type) == 0)
{
{
int __tmp_46 = req_a;
int i = __tmp_46;
int x;
if (i == 0)
{
x = s_memory0;
goto label_112296;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_112296;
}
else 
{
{
__VERIFIER_error();
}
label_112296:; 
 __return_112306 = x;
}
rsp_d = __return_112306;
rsp_status = 1;
goto label_112226;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_47 = req_a;
int __tmp_48 = req_d;
int i = __tmp_47;
int v = __tmp_48;
if (i == 0)
{
s_memory0 = v;
goto label_112252;
}
else 
{
if (i == 1)
{
s_memory1 = v;
goto label_112252;
}
else 
{
{
__VERIFIER_error();
}
label_112252:; 
}
rsp_status = 1;
goto label_112226;
}
}
}
else 
{
rsp_status = 0;
label_112226:; 
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_112376;
}
else 
{
label_112376:; 
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_112422:; 
label_112424:; 
label_112430:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_112424;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_112422;
}
}
else 
{
goto label_112430;
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_112578;
}
else 
{
label_112578:; 
c_read_rsp_ev = 2;
if (c_m_lock == 0)
{
{
__VERIFIER_error();
}
goto label_112592;
}
else 
{
label_112592:; 
c_m_lock = 0;
c_m_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_112650;
}
else 
{
label_112650:; 
c_m_ev = 2;
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_112676;
}
else 
{
{
__VERIFIER_error();
}
label_112676:; 
a = a + 1;
if (a < 2)
{
req_type___0 = 0;
req_a___0 = a;
if (c_m_lock == 1)
{
m_run_st = 2;
m_run_pc = 4;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
else 
{
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
if (((int)c_write_req_ev) == 1)
{
s_run_st = 0;
goto label_112780;
}
else 
{
label_112780:; 
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
goto label_112088;
}
label_112872:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_113134;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_113134:; 
goto label_112890;
}
}
else 
{
label_112890:; 
if (((int)s_run_st) == 0)
{
goto label_112872;
}
else 
{
}
goto label_95476;
}
}
else 
{
}
label_112868:; 
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_113132;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_113132:; 
goto label_112888;
}
}
else 
{
label_112888:; 
if (((int)s_run_st) == 0)
{
goto label_112868;
}
else 
{
}
goto label_95502;
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
}
else 
{
label_112094:; 
goto label_112064;
}
}
else 
{
}
goto label_95556;
}
}
else 
{
}
__retres1 = 0;
 __return_113502 = __retres1;
return 1;
}
}
}
if (((int)s_run_st) == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_94728;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_94728:; 
goto label_94154;
}
}
else 
{
label_94154:; 
goto label_95458;
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
else 
{
label_92464:; 
if (((int)s_run_st) == 0)
{
goto label_92446;
}
else 
{
}
kernel_st = 2;
kernel_st = 3;
if (((int)s_run_st) == 0)
{
goto label_109262;
}
else 
{
}
__retres1 = 0;
goto label_113478;
}
}
}
}
}
}
