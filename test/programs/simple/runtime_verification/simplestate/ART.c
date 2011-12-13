#include <stdio.h>
int __MONITOR_START_TRANSITION   = 0;
int __MONITOR_END_TRANSITION   = 0;
int k  ;
extern int ( /* missing proto */  nondet_int)() ;
int __BLAST_error  ;
void __initialize__(void) ;
extern int k ;
extern int ( /* missing proto */  anti_op)() ;
extern int ( /* missing proto */  nondet_int)() ;
int __return_1745;
int __return_1747;
int __return_1749;
int __return_1596;
int __return_1617;
int __return_1743;
int entry(void)
{
    int i ;
    int j ;
    int tmp ;
    k = 0;
    tmp = nondet_int();
    if (!(tmp))
    {
        j = 0;
        if (!(j < 100))
        {
             __return_1745 = 1;
            goto label_1743;
        }
        else if (j < 100)
        {
            k ++;
            j ++;
            if (!(j < 100))
            {
                 __return_1747 = 1;
                goto label_1743;
            }
            else if (j < 100)
            {
                k ++;
                j ++;
                label_1509:; 
                if (!(j < 100))
                {
                     __return_1749 = 1;
                    goto label_1743;
                }
                else if (j < 100)
                {
                    k ++;
                    __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
                    {
                        int tmp ;
                        int tmp___0 ;
                        {
                            int result ;
                            int mybool ;
                            int tmp ;
                            result = k;
                            tmp = nondet_int();
                            mybool = tmp;
                            if (!(mybool))
                            {
                                result -= 13;
                                label_1532:; 
                                if (!(! mybool))
                                {
                                    result -= 13;
                                    label_1539:; 
                                    if (!(mybool))
                                    {
                                        result -= 13;
                                        label_1546:; 
                                        if (!(! mybool))
                                        {
                                            result -= 13;
                                            label_1553:; 
                                            if (!(mybool))
                                            {
                                                result -= 13;
                                                label_1560:; 
                                                if (!(! mybool))
                                                {
                                                    result -= 13;
                                                    label_1567:; 
                                                    if (!(mybool))
                                                    {
                                                        result -= 13;
                                                        label_1574:; 
                                                        if (!(! mybool))
                                                        {
                                                            result -= 13;
                                                            label_1581:; 
                                                            if (!(mybool))
                                                            {
                                                                result -= 13;
                                                                label_1588:; 
                                                                if (!(! mybool))
                                                                {
                                                                    result -= 13;
                                                                    label_1595:; 
                                                                     __return_1596 = result > 0;
                                                                }
                                                                else if (! mybool)
                                                                {
                                                                    result += 26;
                                                                    goto label_1595;
                                                                }
                                                                tmp = __return_1596;
                                                                if (!(tmp))
                                                                {
                                                                    tmp___0 = 1;
                                                                    label_1616:; 
                                                                     __return_1617 = tmp___0;
                                                                }
                                                                else if (tmp)
                                                                {
                                                                    if (!(k < 0))
                                                                    {
                                                                        if (!(k > 100))
                                                                        {
                                                                            tmp___0 = 1;
                                                                            label_1610:; 
                                                                            label_1612:; 
                                                                            goto label_1616;
                                                                        }
                                                                        else if (k > 100)
                                                                        {
                                                                            tmp___0 = 0;
                                                                            goto label_1610;
                                                                        }
                                                                    }
                                                                    else if (k < 0)
                                                                    {
                                                                        tmp___0 = 0;
                                                                        goto label_1612;
                                                                    }
                                                                }
                                                                tmp = __return_1617;
                                                                if (!(! tmp))
                                                                {
                                                                    __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                    j ++;
                                                                    goto label_1509;
                                                                }
                                                                else if (! tmp)
                                                                {
                                                                    {
                                                                        assert(0); // target state
                                                                    }
                                                                }
                                                            }
                                                            else if (mybool)
                                                            {
                                                                result += 26;
                                                                goto label_1588;
                                                            }
                                                        }
                                                        else if (! mybool)
                                                        {
                                                            result += 26;
                                                            goto label_1581;
                                                        }
                                                    }
                                                    else if (mybool)
                                                    {
                                                        result += 26;
                                                        goto label_1574;
                                                    }
                                                }
                                                else if (! mybool)
                                                {
                                                    result += 26;
                                                    goto label_1567;
                                                }
                                            }
                                            else if (mybool)
                                            {
                                                result += 26;
                                                goto label_1560;
                                            }
                                        }
                                        else if (! mybool)
                                        {
                                            result += 26;
                                            goto label_1553;
                                        }
                                    }
                                    else if (mybool)
                                    {
                                        result += 26;
                                        goto label_1546;
                                    }
                                }
                                else if (! mybool)
                                {
                                    result += 26;
                                    goto label_1539;
                                }
                            }
                            else if (mybool)
                            {
                                result += 26;
                                goto label_1532;
                            }
                        }
                    }
                }
            }
        }
    }
    else if (tmp)
    {
        i = 0;
        label_361:; 
        if (!(i < 1000000))
        {
             __return_1743 = 1;
            label_1743:; 
        }
        else if (i < 1000000)
        {
            anti_op();;
            i ++;
            goto label_361;
        }
    }
}
