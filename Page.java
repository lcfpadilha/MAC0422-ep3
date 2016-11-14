import java.util.*;

public class Page {
   int t_access;
   int page;
   int proc_pid;

   public Page (int page, int t_access, int proc_pid) {
      this.t_access = t_access;
      this.page     = page;
      this.proc_pid = proc_pid;
    }
}