import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

class ProcessComparator implements Comparator<Process> {
   @Override
   public int compare(Process p1, Process p2) {
      if (p1.active) {
         if (p1.t0 > p2.t0)
            return 1;
         else
            return -1;
      }
      else {
         if (p1.tf > p2.tf)
            return 1;
         else
            return -1;
      }
   }
}

class PageComparator implements Comparator<Page> {
   @Override
   public int compare(Page p1, Page p2) {
      if (p1.t_access > p2.t_access)
         return 1;
      else
         return -1;
   }
}


public class EP3 {
   static BitSet virtual_memory_bm;             //bitmap da memoria virtual (usado para alocaçao)
   static int[] real_memory;                    //tabela com quadros de paginas
   static PageTable[] pages_table;              //tabela de páginas
   static LinkedList<Process> next_process;     //processos que serão executados
   static LinkedList<Process> finished_process; //processos que estão em execução
   static LinkedList<Page>    next_pages;       //próximas páginas a serem acessadas 
   static LinkedList<Page>    present_pages;    //listas de páginas que estão na memória física 
   static int alg_space, alg_pages;                     
   static int virtual_size, real_size, allocUnit_size, page_size;
   static int next_fit_index, clock_index;
   static double interval;

   public static void set_traceFile (String f_name) {
      int k = 0;

      // Leitura do arquivo com nome f_name
      try {
         File file   = new File(f_name);
         Scanner in  = new Scanner(file);
         String line = in.nextLine();
         Scanner s   = new Scanner(line);

         // Inicialização das variaveis globais
         next_process     = new LinkedList<Process>();
         next_pages       = new LinkedList<Page>();
         finished_process = new LinkedList<Process>();

         // Leitura da primeira linha
         real_size      = s.nextInt(); 
         virtual_size   = s.nextInt();
         allocUnit_size = s.nextInt();
         page_size      = s.nextInt();
         
         // Inicialização do bitmap da memoria virtual
         virtual_memory_bm = new BitSet(virtual_size / allocUnit_size);
         virtual_memory_bm.clear();

         // Tabela de páginas (vsize / page_size)
         pages_table = new PageTable[virtual_size / page_size];

         // Inicialização do bitmap da memoria real (um bit por pagina)
         real_memory = new int[real_size / page_size];

         // Enquanto houver processos no arquivo de trace
         while (in.hasNextLine()) {
            line = in.nextLine();

            // Leitura dos dados do processo
            s = new Scanner(line);
            int      t0 = s.nextInt();
            String name = s.next(); //TODO: Ajustar o nome
            int      tf = s.nextInt();
            int       b = s.nextInt();

            //Leitura das paginas que serão acessadas pelo processo
            LinkedList<Page> p_pages = new LinkedList<Page>();

            while (s.hasNextInt()) {
               int p = s.nextInt();
               int t = s.nextInt();

               p_pages.add(new Page(p, t, k));
            }
            // Adicionamos o processo a lista ligada de processos prontos
            next_process.add(new Process(k++, t0, name, tf, b, p_pages));
         }
         // Setando tabela de páginas
         for (int i = 0; i < virtual_size / page_size; i++) 
            pages_table[i] = new PageTable();

         for (int i = 0; i < real_memory.length; i++)
            real_memory[i] = -1;

      // Se houve erro
      } catch (FileNotFoundException fnfe) {
        System.out.println("ERRO:" + fnfe);
      }
   } 

   public static void print_virtualPage () {
      System.out.println ("ESTADO DA MEMORIA VIRTUAL:");
      for (int i = 0; i < pages_table.length; i++) {
         System.out.print (pages_table[i].proc_pid/* + "(" + pages_table[i].page_frame+", " + pages_table[i].presence + ", " + pages_table[i].label + ")"*/+ " ");
      }
      System.out.println ();
   }

   public static void print_realPage() {
      System.out.println ("ESTADO DA MEMORIA REAL:");
      for (int i = 0; i < real_memory.length; i++)
         System.out.print (real_memory[i] + " ");

      System.out.println ();
   }

   public static int alloc_memory (Process proc) {
      int i, alloc_size, max_units, k, pages, page_ini = 0, page_end = 0;
      max_units = virtual_memory_bm.size();
      alloc_size = (int) Math.ceil( (double) proc.b/allocUnit_size); 
      k = max_units - alloc_size;

      // Alocando por First Fit
      if (alg_space == 1) {

         for (i = 0; i < max_units - alloc_size; i++) {
            if (!virtual_memory_bm.get(i)) {
               k = i + 1;
               while (k - i < alloc_size && !virtual_memory_bm.get(k))
                  k++;
               if (k - i == alloc_size) break;
               else k = max_units - alloc_size;
            }
         }
         // Garantido que existe k e i
         for (int j = i; j < k; j++) 
            virtual_memory_bm.set(j);

         // Habilitando tabela de páginas
         page_ini = i * allocUnit_size / page_size;
         page_end = k * allocUnit_size / page_size;
         for (int j = page_ini; j < page_end; j++) {
            pages_table[j].proc_pid = proc.pid; // Identificador do processo que está ocupando essa posição
            pages_table[j].presence = false;    // Bit de ausente/presente
            pages_table[j].r = false;           // Bit R
         }
      }
      // Alocando por Next Fit
      else if (alg_space == 2) {

         for (i = next_fit_index; i - next_fit_index < max_units; i++) {
            if (!virtual_memory_bm.get(i)) {
               k = i + 1;
               while (k - i < alloc_size && k < max_units && !virtual_memory_bm.get(k))
                  k++;
               if (k - i == alloc_size) break;
            }
         }

         // Existem k e i
         for (int j = i; j < k; j++)
            virtual_memory_bm.set(j);

         // Habilitando tabela de paginas
         page_ini = i * allocUnit_size / page_size;
         page_end = k * allocUnit_size / page_size;
         for (int j = page_ini; j < page_end; j++) {
            pages_table[j].proc_pid = proc.pid;
            pages_table[j].presence = false;
            pages_table[j].r = false;
         }

         // Guardamos o ultimo indice acessado 
         next_fit_index = k % max_units;
      }
      // Alocando por Best Fit
      else if (alg_space == 3) {
         int min_free = 0, ini = 0;

         for (i = 0; i < max_units; i++) {
            if (!virtual_memory_bm.get(i)) {
               int free = 0;
               k = i + 1;
               while (k < max_units && !virtual_memory_bm.get(k)) {
                  free++; k++;
               }
               
               if (min_free == 0) min_free = free;

               if (min_free >= free && free >= alloc_size) {
                  min_free = free;
                  ini = i;
                  i = k;
               }
            }
         }

         // Existem k e i
         for (int j = ini; j - ini < alloc_size; j++)
            virtual_memory_bm.set(j);

         // Habilitando tabela de pagina
         page_ini = ini * allocUnit_size / page_size;
         pages = alloc_size * allocUnit_size / page_size;

         for (int j = page_ini; j - page_ini < pages; j++) {
            pages_table[j].proc_pid = proc.pid;
            pages_table[j].presence = false;
            pages_table[j].r = false;
         }
      }
      // Alocando por Worst Fit
      else {
         int max_free = 0, ini = 0;

         for (i = 0; i < max_units; i++) {
            if (!virtual_memory_bm.get(i)) {
               int free = 0;
               k = i + 1;
               while (k < max_units && !virtual_memory_bm.get(k)) {
                  free++; k++;
               }
               if (max_free < free && free >= alloc_size) {
                  max_free = free;
                  ini = i;
                  i = k;
               }
            }
         }

         // Existem k e i
         for (int j = ini; j - ini < alloc_size; j++)
            virtual_memory_bm.set(j);

         // Habilitando tabela de paginas
         page_ini = ini * allocUnit_size / page_size;
         pages = alloc_size * allocUnit_size / page_size;

         for (int j = page_ini; j - page_ini < pages; j++) {
            pages_table[j].proc_pid = proc.pid;
            pages_table[j].presence = false;
            pages_table[j].r = false;
         }
      }

      print_virtualPage();

      return page_ini;
   }

   public static int alloc_realMemory (Page p) {
      int size = real_memory.length;
      for (int i = 0; i < size; i++) {
         if (real_memory[i] == -1) {
            real_memory[i] = p.proc_pid;
            return i;
         }
      }
      return -1;
   }

   public static int page_fault (Page p) {
      int page = 0;
      // Optimal
      if (alg_pages == 1) {
         int max = 0;
         page = -1;
         for (int i = 1; i < pages_table.length; i++)
            if (max < pages_table[i].label) {
               max = pages_table[i].label;
               page = i;
            }
      }
      
      // Second-Chance
      else if (alg_pages == 2) {
         while (true) {
            Page first = present_pages.removeFirst();

            if (pages_table[first.page].r) {
               pages_table[first.page].r = false;
               present_pages.add(first);
            }
            else {
               page = first.page;
               break;
            }
         }  
      }
      // Clock
      else if (alg_pages == 3) {
         while (true) {
            Page actual = present_pages.get(clock_index);

            if (pages_table[actual.page].r) {
               pages_table[actual.page].r = false;
            }
            else {
               present_pages.remove(clock_index);
               page = actual.page;
               clock_index = (clock_index) % present_pages.size();
               break;
            }
            clock_index = (clock_index + 1) % present_pages.size();
         }  
      }
      /*
      // LRU
      else if (alg_pages == 4) {

      }*/

      return page;
   }

   public static int[] select_event (int tmin) {
      int[] event = new int[2];

      if (!next_process.isEmpty()) {
         Process p = next_process.element();
         tmin = p.t0;
         event[1] = 1;
      }

      if (!next_pages.isEmpty()) {
         if (tmin == -1 || next_pages.element().t_access < tmin) {
            Page p = next_pages.element();
            tmin = p.t_access;
            event[1] = 2;
         }
      }

      if (!finished_process.isEmpty()) {
         if (tmin == -1 || finished_process.element().tf < tmin) {
            Process p = finished_process.element(); 
            tmin = p.tf;
            event[1] = 3;
         }
      }
      event[0] = tmin;
      
      return event;
   }

   public static void init_simulator () {
      int time = 0;

      if (alg_space == 2) next_fit_index = 0;
      if (alg_pages == 3) clock_index = 0;
      
      System.out.println ("[Processo]\t[PID]");

      for (int i = 0; i < next_process.size(); i++)
         System.out.println ("[" + next_process.get(i).name + "]\t[" + next_process.get(i).pid + "]");

      if (alg_pages == 2 || alg_pages == 3) 
         present_pages = new LinkedList<Page>();

      while (!next_process.isEmpty() || !next_pages.isEmpty() || !finished_process.isEmpty()) {
         int tmin = -1;
         int event[];

         // Seleciona o próximo evento (aquele com tempo menor)
         event = select_event (tmin);
         tmin = event[0];

         while (tmin != -1 && tmin < time + interval) {
            // Evento 1 -> Processo entrando no sistema
            if (event[1] == 1) {

               Process proc_next = next_process.removeFirst();
               System.out.println (tmin + ": inserindo processo " + proc_next.name);
               
               proc_next.first_page = alloc_memory (proc_next);
               proc_next.active = true;
               while (!proc_next.pages.isEmpty()) {
                  Page new_page = proc_next.pages.removeFirst();
                  new_page.page = proc_next.first_page + new_page.page/page_size;

                  next_pages.add(new_page);
               }

               finished_process.add(proc_next);
               Collections.sort(finished_process, new ProcessComparator());
               Collections.sort(next_pages, new PageComparator());

               // Se o alogirtmo for Optimal, a gente coloca os rotulos nas
               //paginas
               if (alg_pages == 1) {
                  for (Page p : next_pages)
                     pages_table[p.page].label = 0;

                  int i = 1;
                  for (Page p : next_pages) {
                     if (pages_table[p.page].label == 0)
                        pages_table[p.page].label = i;
                     i++;
                  }
               }
            }
            // Evento 2 -> Pagina querendo ser acessada
            else if (event[1] == 2) {
               Page p = next_pages.removeFirst();
               System.out.println (tmin + ": acessando pagina " + p.page + " do processo " + p.proc_pid);
               
               if (!pages_table[p.page].presence) {
                  int page_frame = alloc_realMemory(p);
               
                  if (page_frame == -1) {
                     int  removed_page = page_fault (p);

                     // Colocamos o p_id do processo no page_frame da página antiga
                     real_memory[pages_table[removed_page].page_frame] = p.proc_pid;

                     // Gravamos o novo quadro de pagina em page_frame
                     pages_table[removed_page].presence = false;
                     page_frame = pages_table[removed_page].page_frame;
                  }

                  // Registramos o uso do quadro de página page_frame
                  // para a página p
                  pages_table[p.page].page_frame = page_frame;
                  pages_table[p.page].presence   = true;
                  pages_table[p.page].r          = true;

                  if (alg_pages == 2 || alg_pages == 3)
                     present_pages.add(p);

                  print_realPage();
                  print_virtualPage();
               }
               else 
                  pages_table[p.page].r = true;

               // Determina o tempo para a página envelhecer
               pages_table[p.page].aging_time = time + 5;

            }
            // Evento 3 -> Processo saindo do sistema
            else if (event[1] == 3) {
               Process proc_end = finished_process.removeFirst();
               System.out.println (tmin + ": terminando execucao do " + proc_end.name);

               // Limpando memória virtual
               for (int i = proc_end.first_page; pages_table[i].proc_pid == proc_end.pid; i++)
                  pages_table[i].proc_pid = -1;

               int first_allocUnit = (proc_end.first_page * page_size) / allocUnit_size;
               int last_allocUnit  = (int) Math.ceil( (double) proc_end.b / allocUnit_size); 

               for (int i = first_allocUnit; i - first_allocUnit < last_allocUnit; i++) 
                  virtual_memory_bm.clear(i);

               print_virtualPage();
            }
            event = select_event (-1);
            tmin = event[0];
         }

         // Envelhecemos as páginas que estão presentes
         if (alg_pages == 2 || alg_pages == 3) 
            for (Page p : present_pages)
               if (time < pages_table[p.page].aging_time && pages_table[p.page].aging_time <= interval)
                  pages_table[p.page].r = false;
         time += interval;
      }
   }

   public static void execute_command (String[] s) {
      //Carrega arquivo de trace no simulador
      if (s[0].equals("carrega")) {
         set_traceFile(s[1]);
         System.out.println ("console: trace carregado!");
      }
      //Define o algoritmo de alocação
      else if (s[0].equals("espaco")) {
         alg_space = Integer.parseInt(s[1]);
         System.out.println ("console: algoritmo de gerencia de espaco livre numero " + alg_space + " selecionado!");
         //sim.set_allocManagement(s[1]);
      }
      //Define o algoritmo de paginação
      else if (s[0].equals("substitui")) {
         alg_pages = Integer.parseInt(s[1]);
         System.out.println ("console: algoritmo de paginação numero " + alg_space + " selecionado!");
         //sim.set_pageManagement(s[1]);
      }
      //Intervalo de tempo
      else if (s[0].equals("intervalo")) {
         interval = Double.parseDouble(s[1]);
         System.out.println ("console: intervalo selecionado!");
      }
      //Inicia simulador
      else if (s[0].equals("executa")) {
         System.out.println ("console: executando o simulador!");
         init_simulator();
         //sim.init();
      }
      else {
         System.out.println ("Comando INVÁLIDO!");
      }
   }

   public static void main (String[] args) {
      Scanner in = new Scanner(System.in);
      String[] com_args;

      // Inicio do prompt
      while (true) {
         System.out.print("(ep3): ");
         if (!in.hasNextLine()) break;
         
         // Lemos os argumentos da linha
         com_args = in.nextLine().split("\\s+");

         // Executamos o comando passado
         if (com_args.length > 0 && com_args[0].equals("sai"))
            break;
         else if (com_args.length > 0)
           execute_command(com_args);
      }
   }
}