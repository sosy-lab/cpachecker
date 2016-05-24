extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int main_in1_val  ;
int main_in1_val_t  ;
int main_in1_ev  ;
int main_in1_req_up  ;
int main_in2_val  ;
int main_in2_val_t  ;
int main_in2_ev  ;
int main_in2_req_up  ;
int main_diff_val  ;
int main_diff_val_t  ;
int main_diff_ev  ;
int main_diff_req_up ;
int main_sum_val  ;
int main_sum_val_t  ;
int main_sum_ev  ;
int main_sum_req_up ;
int main_pres_val  ;
int main_pres_val_t  ;
int main_pres_ev  ;
int main_pres_req_up  ;
int main_dbl_val  ;
int main_dbl_val_t  ;
int main_dbl_ev  ;
int main_dbl_req_up  ;
int main_zero_val  ;
int main_zero_val_t  ;
int main_zero_ev  ;
int main_zero_req_up  ;
int main_clk_val  ;
int main_clk_val_t  ;
int main_clk_ev  ;
int main_clk_req_up  ;
int main_clk_pos_edge  ;
int main_clk_neg_edge  ;
int N_generate_st  ;
int N_generate_i  ;
int S1_addsub_st  ;
int S1_addsub_i  ;
int S2_presdbl_st  ;
int S2_presdbl_i  ;
int S3_zero_st  ;
int S3_zero_i  ;
int D_z  ;
int D_print_st  ;
int D_print_i  ;
int flag = 0;
void N_generate(void);
void S1_addsub(void);
void S2_presdbl(void);
void S3_zero(void);
void D_print(void);
void eval(void);
void start_simulation(void);
int main(void);
int main()
{
int count = __VERIFIER_nondet_int();
count = __VERIFIER_nondet_int();
int __retres2 ;
main_in1_ev = 2;
main_in1_req_up = 0;
main_in2_ev = 2;
main_in2_req_up = 0;
main_diff_ev = 2;
main_diff_req_up = 0;
main_sum_ev = 2;
main_sum_req_up = 0;
main_pres_ev = 2;
main_pres_req_up = 0;
main_dbl_ev = 2;
main_dbl_req_up = 0;
main_zero_ev = 2;
main_zero_req_up = 0;
main_clk_val = 0;
main_clk_ev = 2;
main_clk_req_up = 0;
main_clk_pos_edge = 2;
main_clk_neg_edge = 2;
count = 0;
N_generate_i = 0;
S1_addsub_i = 0;
S2_presdbl_i = 0;
S3_zero_i = 0;
D_print_i = 0;
{
int kernel_st ;
kernel_st = 0;
N_generate_st = 2;
S1_addsub_st = 2;
S2_presdbl_st = 2;
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_844714;
}
else 
{
label_844714:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_844721;
}
else 
{
label_844721:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_844728;
}
else 
{
label_844728:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_844735;
}
else 
{
label_844735:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_844742;
}
else 
{
label_844742:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_844749;
}
else 
{
label_844749:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_844756;
}
else 
{
label_844756:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_844763;
}
else 
{
label_844763:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_847831;
}
else 
{
label_847831:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_847868;
}
else 
{
label_847868:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_847875;
}
else 
{
label_847875:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_847882;
}
else 
{
label_847882:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_847889;
}
else 
{
label_847889:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_847896;
}
else 
{
label_847896:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_847903;
}
else 
{
label_847903:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_847910;
}
else 
{
label_847910:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_847917;
}
else 
{
label_847917:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_847930;
}
else 
{
label_847930:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_848063;
}
else 
{
label_848063:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_848070;
}
else 
{
label_848070:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_848077;
}
else 
{
label_848077:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_848084;
}
else 
{
label_848084:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_848091;
}
else 
{
label_848091:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_848098;
}
else 
{
label_848098:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_848105;
}
else 
{
label_848105:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_848112;
}
else 
{
label_848112:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_848125;
}
else 
{
label_848125:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_848162;
}
else 
{
label_848162:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_848169;
}
else 
{
label_848169:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_848176;
}
else 
{
label_848176:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_848183;
}
else 
{
label_848183:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_848190;
}
else 
{
label_848190:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_848197;
}
else 
{
label_848197:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_848204;
}
else 
{
label_848204:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_848211;
}
else 
{
label_848211:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_848224;
}
else 
{
label_848224:; 
}
label_848249:; 
main_clk_val_t = 1;
main_clk_req_up = 1;
count = count + 1;
if (count == 5)
{
flag = D_z;
count = 0;
goto label_848258;
}
else 
{
label_848258:; 
main_clk_val_t = 0;
main_clk_req_up = 1;
{
int kernel_st ;
kernel_st = 0;
main_clk_req_up = 0;
N_generate_st = 2;
S1_addsub_st = 2;
S2_presdbl_st = 2;
S3_zero_st = 2;
D_print_st = 2;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_848403;
}
else 
{
label_848403:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_848410;
}
else 
{
label_848410:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_848417;
}
else 
{
label_848417:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_848424;
}
else 
{
label_848424:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_848431;
}
else 
{
label_848431:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_848438;
}
else 
{
label_848438:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_848445;
}
else 
{
label_848445:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_848452;
}
else 
{
label_848452:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_848465;
}
else 
{
label_848465:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_848502;
}
else 
{
label_848502:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_848509;
}
else 
{
label_848509:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_848516;
}
else 
{
label_848516:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_848523;
}
else 
{
label_848523:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_848530;
}
else 
{
label_848530:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_848537;
}
else 
{
label_848537:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_848544;
}
else 
{
label_848544:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_848551;
}
else 
{
label_848551:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_848564;
}
else 
{
label_848564:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
int tmp___3 ;
}
kernel_st = 2;
kernel_st = 3;
if (((int)main_in1_ev) == 0)
{
main_in1_ev = 1;
goto label_848697;
}
else 
{
label_848697:; 
if (((int)main_in2_ev) == 0)
{
main_in2_ev = 1;
goto label_848704;
}
else 
{
label_848704:; 
if (((int)main_sum_ev) == 0)
{
main_sum_ev = 1;
goto label_848711;
}
else 
{
label_848711:; 
if (((int)main_diff_ev) == 0)
{
main_diff_ev = 1;
goto label_848718;
}
else 
{
label_848718:; 
if (((int)main_pres_ev) == 0)
{
main_pres_ev = 1;
goto label_848725;
}
else 
{
label_848725:; 
if (((int)main_dbl_ev) == 0)
{
main_dbl_ev = 1;
goto label_848732;
}
else 
{
label_848732:; 
if (((int)main_zero_ev) == 0)
{
main_zero_ev = 1;
goto label_848739;
}
else 
{
label_848739:; 
if (((int)main_clk_ev) == 0)
{
main_clk_ev = 1;
goto label_848746;
}
else 
{
label_848746:; 
if (((int)main_clk_neg_edge) == 0)
{
main_clk_neg_edge = 1;
goto label_848759;
}
else 
{
label_848759:; 
if (((int)main_in1_ev) == 1)
{
main_in1_ev = 2;
goto label_848796;
}
else 
{
label_848796:; 
if (((int)main_in2_ev) == 1)
{
main_in2_ev = 2;
goto label_848803;
}
else 
{
label_848803:; 
if (((int)main_sum_ev) == 1)
{
main_sum_ev = 2;
goto label_848810;
}
else 
{
label_848810:; 
if (((int)main_diff_ev) == 1)
{
main_diff_ev = 2;
goto label_848817;
}
else 
{
label_848817:; 
if (((int)main_pres_ev) == 1)
{
main_pres_ev = 2;
goto label_848824;
}
else 
{
label_848824:; 
if (((int)main_dbl_ev) == 1)
{
main_dbl_ev = 2;
goto label_848831;
}
else 
{
label_848831:; 
if (((int)main_zero_ev) == 1)
{
main_zero_ev = 2;
goto label_848838;
}
else 
{
label_848838:; 
if (((int)main_clk_ev) == 1)
{
main_clk_ev = 2;
goto label_848845;
}
else 
{
label_848845:; 
if (((int)main_clk_neg_edge) == 1)
{
main_clk_neg_edge = 2;
goto label_848858;
}
else 
{
label_848858:; 
}
goto label_848249;
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
}
}
}
}
