import java.util.*;

public class Process {
   int t0;
   int tf;
   int b;
   String name;
   LinkedList<Page> pages;

   public Process (int t0, String name, int tf, int b, LinkedList<Page> pages) {
      this.t0    = t0;
      this.tf    = tf;
      this.name  = name;
      this.pages = pages;
   }
}