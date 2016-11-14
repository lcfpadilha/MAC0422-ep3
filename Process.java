import java.util.*;

public class Process {
   int pid;
   int t0;
   int tf;
   int b;
   int first_page;
   boolean active;
   String name;
   LinkedList<Page> pages;

   public Process (int pid, int t0, String name, int tf, int b, LinkedList<Page> pages) {
      this.pid   = pid;
      this.t0    = t0;
      this.tf    = tf;
      this.b     = b;
      this.name  = name;
      this.pages = pages;
      this.active = false;
   }
}