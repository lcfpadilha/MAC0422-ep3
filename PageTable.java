import java.util.*;

public class PageTable {
    int label;
    int proc_pid;
    int page_frame;
    int aging_time;
    boolean presence;
    boolean r;
    int lru_counter; //contador de 8 bits


   public PageTable () {
    this.proc_pid = -1;
    this.page_frame = 0;
    this.presence = false;
    this.r = false;
    this.label = 0;
    this.lru_counter = 0;
   }
}