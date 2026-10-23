package io.freemytv;
import java.util.*;
public final class AppMetricsTest {
    public static void main(String[] args){
        Map<String,List<Integer>> p=AppMetrics.processes(" PID NAME\r\n 42 com.example.tv\r\n 77 com.example.tv:player\n 8 system_server\n 98 [worker]\n");
        if(p.size()!=1||!p.get("com.example.tv").equals(Arrays.asList(42,77)))throw new AssertionError(p);
        if(AppMetrics.pss("No process found for: com.example.tv")!=null)throw new AssertionError("Missing is not zero");
        if(AppMetrics.pss("Permission Denial: meminfo")!=null)throw new AssertionError("Denied is not zero");
        if(AppMetrics.pss(" TOTAL 6622 3084 124 448\n TOTAL PSS: 6622 TOTAL RSS: 83800")!=6622L*1024)throw new AssertionError("Do not count both table and summary");
        if(AppMetrics.pss(" TOTAL PSS: 6,622\n TOTAL PSS: 1,024")!=7646L*1024)throw new AssertionError("Sum processes");
        if(AppMetrics.pss(" TOTAL 456 234 100\n")!=456L*1024)throw new AssertionError("Old table");
        if(!AppMetrics.bytes(0).equals("0 B")||!AppMetrics.bytes(1048576).equals("1.0 MB")||!AppMetrics.bytes(-1).equals("Chưa đọc được"))throw new AssertionError("Units");
        if(AppMetrics.rate(1000,3000,2000)!=1000||AppMetrics.rate(-1,30,1000)!=-1||AppMetrics.rate(100,20,1000)!=-1||AppMetrics.rate(0,20,0)!=-1)throw new AssertionError("Rate: elapsed time, unsupported counter and reset");
        System.out.println("Process groups, multi-process PSS, unknown/denied values and byte units passed.");
    }
}
