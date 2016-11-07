import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

public class EP3 {
   static BitSet virtual_memory_bm;             //bitmap da memoria virtual (usado para alocaçao)
   static BitSet real_memory_bm;                //bitmap da memoria real (usado para alocaçao)
   static int[][] table_pages;
   static LinkedList<Process> next_process;     //processos que serão executados
   static LinkedList<Process> finished_process; //processos que estão em execução
   static LinkedList<Page> next_pages;          //próximas páginas a serem acessadas
   static int alg_space, alg_pages;
   static double interval;
   static int virtual_size, real_size, allocUnit_size, page_size;
   static int last_Index;

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
         real_size      = s.nextInt(); //TODO: Colocar global?
         virtual_size   = s.nextInt();
         allocUnit_size = s.nextInt();
         page_size      = s.nextInt();
         
         // Inicialização do bitmap da memoria virtual
         virtual_memory_bm = new BitSet(virtual_size / allocUnit_size);
         virtual_memory_bm.clear();

         // Tabela de páginas (vsize / page_size linhas por 3 colunas)
         table_pages = new int[virtual_size / page_size][4];

         // Inicialização do bitmap da memoria real
         real_memory_bm = new BitSet(virtual_size / page_size);
         real_memory_bm.clear();

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

               p_pages.add(new Page(p, t));
            }
            // Adicionamos o processo a lista ligada de processos prontos
            next_process.add(new Process(k++, t0, name, tf, b, p_pages));
         }
         // Setando tabela de páginas
         for (int i = 0; i < virtual_size / page_size; i++)
            table_pages[i][0] = -1;

      // Se houve erro
      } catch (FileNotFoundException fnfe) {
        System.out.println("ERRO:" + fnfe);
      }
   } 

   public static void print_virtualPage () {
      System.out.println ("ESTADO DA MEMORIA VIRTUAL:");
      for (int i = 0; i < table_pages.length; i++) {
         System.out.print (table_pages[i][0] + " ");
      }
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
            table_pages[j][0] = proc.pid;
            table_pages[j][2] = 0;
            table_pages[j][3] = 0;
         }
      }
      // Alocando por Next Fit
      else if (alg_space == 2) {

         for (i = last_Index; i - last_Index < max_units; i++) {
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
            table_pages[j][0] = proc.pid;
            table_pages[j][2] = 0;
            table_pages[j][3] = 0;
         }

         // Guardamos o ultimo indice acessado 
         last_Index = k;
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
            table_pages[j][0] = proc.pid;
            table_pages[j][2] = 0;
            table_pages[j][3] = 0;
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
            table_pages[j][0] = proc.pid;
            table_pages[j][2] = 0;
            table_pages[j][3] = 0;
         }
      }

      print_virtualPage();

      return page_ini;
   }

   public static void page_fault (Page p) {

      // Optimal
      if (alg_pages == 1) {

      }
      // Second-Chance
      else if (alg_pages == 2) {

      }
      // Clock
      else if (alg_pages == 3) {

      }
      // LRU
      else if (alg_pages == 4) {

      }
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
      System.out.println ("[Processo]\t[PID]");

      for (int i = 0; i < next_process.size(); i++)
         System.out.println ("[" + next_process.get(i).name + "]\t[" + next_process.get(i).pid + "]");

      while (!next_process.isEmpty() || !next_pages.isEmpty() || !finished_process.isEmpty()) {
         int tmin = -1;
         int event[];

         // Seleciona o próximo evento (aquele com tempo menor)
         event = select_event (tmin);
         tmin = event[0];

         while (tmin != -1 && tmin < time + interval) {
            if (event[1] == 1) {
               Process proc_next = next_process.removeFirst();
               
               proc_next.first_page = alloc_memory (proc_next);

               while (!proc_next.pages.isEmpty()) {
                  Page new_page = proc_next.pages.removeFirst();
                  new_page.page = proc_next.first_page + new_page.page;

                  next_pages.add(new_page);
               }

               finished_process.add(proc_next);
               // Ordena next_pages
               // Ordena proc_next
            }
            else if (event[1] == 2) {
               Page p = next_pages.removeFirst();
               System.out.println (tmin + ": acessando pagina " + p.page);
               if (table_pages[p.page][3] != 1) {
                  System.out.println ("Page Fault");
                  //page_fault (p);
               }
            }
            else if (event[1] == 3) {
               Process proc_end = finished_process.removeFirst();
               System.out.println (tmin + ": terminando execucao do " + proc_end.name);

               for (int i = proc_end.first_page; table_pages[i][0] == proc_end.pid; i++)
                  table_pages[i][0] = -1;

               int first_allocUnit = (proc_end.first_page * page_size) / allocUnit_size;
               int last_allocUnit  = (int) Math.ceil( (double) proc_end.b / allocUnit_size); 

               for (int i = first_allocUnit; i - first_allocUnit < last_allocUnit; i++) {
                  System.out.println (virtual_memory_bm.get(i) + " " + i);
                  virtual_memory_bm.clear(i);
               }

               print_virtualPage();
            }
            event = select_event (-1);
            tmin = event[0];
         }
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
         System.out.print("(ep2): ");
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