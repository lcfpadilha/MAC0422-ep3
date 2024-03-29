import java.util.*;
import java.io.*;

// Comparador de processos
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

// Comparador de páginas
class PageComparator implements Comparator<Page> {
   @Override
   public int compare(Page p1, Page p2) {
      if (p1.t_access > p2.t_access)
         return 1;
      else if (p1.t_access < p2.t_access)
         return -1;
      else
         return 0;
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
   static LinkedList<Long>  total_time;      
   static int alg_space, alg_pages;                     
   static int virtual_size, real_size, allocUnit_size, page_size;
   static int next_fit_index, clock_index;
   static double interval;

   // set_traceFile: recebe o nome de um arquivo f_name, abre-o e
   //incializa todas as variáveis para o início do simulador
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
            String name = s.next();
            int      tf = s.nextInt();
            int       b = s.nextInt();

            //Leitura das paginas que serão acessadas pelo processo
            LinkedList<Page> p_pages = new LinkedList<Page>();

            while (s.hasNextInt()) {
               int p = s.nextInt() / page_size;
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

         Collections.sort(next_process, new ProcessComparator());

      // Se houve erro
      } catch (FileNotFoundException fnfe) {
        System.out.println("ERRO:" + fnfe);
      }
   } 

   // print_virtualPage: imprime o estado da memória virtual (PID de 
   //cada processo por página) na saída padrão
   public static void print_virtualPage () {
      System.out.println ("ESTADO DA MEMORIA VIRTUAL:");
      for (int i = 0; i < pages_table.length; i++) {
         System.out.print (pages_table[i].proc_pid + " ");
      }
      System.out.println ();
   }

   // print_realPage: imprime o estado da memória real (PID de 
   //cada processo por página) na saída padrão
   public static void print_realPage() {
      System.out.println ("ESTADO DA MEMORIA REAL:");
      for (int i = 0; i < real_memory.length; i++)
         System.out.print (real_memory[i] + " ");

      System.out.println ();
   }

   // print_bitmap: imprime o estado do bitmap
   public static void print_bitmap() {
      System.out.println ("BITMAP DA MEMORIA VIRTUAL:");
      for (int i = 0; i < virtual_memory_bm.size(); i++)
         System.out.print (virtual_memory_bm.get(i) + " ");
      System.out.println ();
   }

   // print_virtual_memory: altera o arquivo /tmp/ep3.vir com a nova
   //memoria.
   public static void print_virtual_memory() throws IOException {
      String data = "";
      for (PageTable pt : pages_table) 
         for (int i = 0; i < page_size; i++)
            data = data + String.format("%16s", Integer.toBinaryString(pt.proc_pid)).replace(' ', '0');


      File file = new File("tmp/ep3.vir");
      
      // creates the file
      file.getParentFile().mkdirs();
      file.createNewFile();

      // creates a FileWriter Object
      FileWriter writer = new FileWriter(file); 
      
      // Writes the content to the file
      writer.write(data); 
      writer.flush();
      writer.close();
   }

   // print_virtual_memory: altera o arquivo /tmp/ep3.vir com a nova
   //memoria.
   public static void print_real_memory() throws IOException {
      String data = "";
      for (int i = 0; i < real_memory.length; i++) 
         for (int j = 0; j < page_size; j++)
            data = data + String.format("%16s", Integer.toBinaryString(real_memory[i])).replace(' ', '0');


      File file = new File("tmp/ep3.mem");
      
      // creates the file
      file.getParentFile().mkdirs();
      file.createNewFile();

      // creates a FileWriter Object
      FileWriter writer = new FileWriter(file); 
      
      // Writes the content to the file
      writer.write(data); 
      writer.flush();
      writer.close();
   }
   // alloc_memory: recebe um Process proc e aloca espaço na
   //memória virtual para ele utilizando um dos algoritmos
   //de gerência de espaço livre
   public static int alloc_memory (Process proc) {
      int i, alloc_size, max_units, k, pages, page_ini = 0, page_end = 0;
      max_units = virtual_size / allocUnit_size;
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

         for (int j = page_ini; j < pages_table.length && j < page_end; j++) {
            pages_table[j].proc_pid = proc.pid; // Identificador do processo que está ocupando essa posição
            pages_table[j].presence = false;    // Bit de ausente/presente
            pages_table[j].r = false;           // Bit R
         }
      }
      // Alocando por Next Fit
      else if (alg_space == 2) {

         for (i = next_fit_index; i - next_fit_index < max_units; i++) {
            int index = i % max_units;
            if (!virtual_memory_bm.get(index)) {
               k = index + 1;
               while (k - index < alloc_size && k < max_units && !virtual_memory_bm.get(k))
                  k++;
               if (k - index == alloc_size) break;
            }
         }
         i = i % max_units;
         
         // Existem k e i
         for (int j = i; j < k; j++)
            virtual_memory_bm.set(j);

         // Habilitando tabela de paginas
         page_ini = i * allocUnit_size / page_size;
         page_end = k * allocUnit_size / page_size;

         for (int j = page_ini; j < pages_table.length && j < page_end; j++) {
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

         for (int j = page_ini; j < pages_table.length && j - page_ini < pages; j++) {
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
         for (int j = ini; j < pages_table.length && j - ini < alloc_size; j++)
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

      return page_ini;
   }

   // alloc_realMemory: recebe uma página p e aloca espaço na
   //memória real para essa página (retorna -1 se não houve
   //possibilidade de alocação)
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

   // page_fault: recebe uma Page p, retira alguma página
   //que está na memória real e substitui pela página p.
   //A escolha da página a ser retirada é através de um 
   //dos algoritmos de substituição de página. 
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
      
      // LRU
      else if (alg_pages == 4) {

         //vamos procurar a página com menor contador
         Page lesser = present_pages.get(0);
         for(Page pg : present_pages) {
            if(pages_table[pg.page].lru_counter < pages_table[lesser.page].lru_counter)
               lesser = pg;
         }

         present_pages.remove(lesser);
         page = lesser.page;
      }

      return page;
   }

   // select_event: recebe um inteiro tmin e escolhe
   //o menor tempo entre as 3 filas de próximos 
   //eventos
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

   // init_simulator: inicia a simulação da gerencia de memoria
   public static void init_simulator () throws IOException {
      int time = 0;
      int fault_count = 0;
      int max = 0;

      if (alg_space == 2) next_fit_index = 0;
      if (alg_pages == 3) clock_index = 0;
      total_time = new LinkedList<Long>();
      System.out.println ("[Processo]\t[PID]");

      for (int i = 0; i < next_process.size(); i++)
         System.out.println ("[" + next_process.get(i).name + "]\t[" + next_process.get(i).pid + "]");

      if (alg_pages != 1) 
         present_pages = new LinkedList<Page>();

      //print_virtual_memory();
      //print_real_memory();

      while (!next_process.isEmpty() || !next_pages.isEmpty() || !finished_process.isEmpty()) {
         int tmin = -1;
         int event[];

         // Seleciona o próximo evento (aquele com tempo menor)
         event = select_event (tmin);
         tmin = event[0];

         while (tmin != -1 && tmin < time + interval) {
            // Evento 1 -> Processo entrando no sistema
            if (event[1] == 1) {
               Chronometer ch = new Chronometer ();
               Process proc_next = next_process.removeFirst();
               ch.start();
               proc_next.first_page = alloc_memory (proc_next);
               ch.stop();
               total_time.add(ch.getTime());
               proc_next.active = true;

               while (!proc_next.pages.isEmpty()) {
                  Page new_page = proc_next.pages.removeFirst();
                  new_page.page = proc_next.first_page + new_page.page/page_size;
                  next_pages.add(new_page);
               }

               finished_process.add(proc_next);
               Collections.sort(next_process, new ProcessComparator());
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
               // Imprimimos a nova configuração da página virtual
               //print_virtual_memory();
            }
            // Evento 2 -> Pagina querendo ser acessada
            else if (event[1] == 2) {
               Page p = next_pages.removeFirst();
               
               if (!pages_table[p.page].presence) {
                  int page_frame = alloc_realMemory(p);
               
                  if (page_frame == -1) {
                     int  removed_page = page_fault (p);
                     fault_count++;
                     // Colocamos o p_id do processo no page_frame da página antiga
                     real_memory[pages_table[removed_page].page_frame] = p.proc_pid;

                     // Gravamos o novo quadro de pagina em page_frame
                     pages_table[removed_page].presence = false;
                     page_frame = pages_table[removed_page].page_frame;
                  }

                  // Registramos o uso do quadro de página page_frame
                  // para a página p
                  pages_table[p.page].page_frame  = page_frame;
                  pages_table[p.page].presence    = true;
                  pages_table[p.page].r           = true;
                  pages_table[p.page].lru_counter = 0; //zeramos o contador do LRU

                  // Imprimimos a nova configuração da memória real

                  //print_real_memory();

                  if (alg_pages != 1)
                     present_pages.add(p);
               }
               else 
                  pages_table[p.page].r = true;

               // Determina o tempo para a página envelhecer
               pages_table[p.page].aging_time = time + 5;

            }
            // Evento 3 -> Processo saindo do sistema
            else if (event[1] == 3) {
               Process proc_end = finished_process.removeFirst();

               // Limpando memória virtual
               for (int i = proc_end.first_page; i < pages_table.length && pages_table[i].proc_pid == proc_end.pid; i++) {
                  pages_table[i].proc_pid = -1;
                  if (pages_table[i].presence)
                     real_memory[pages_table[i].page_frame] = -1;
               }

               int first_allocUnit = (proc_end.first_page * page_size) / allocUnit_size;
               int last_allocUnit  = (int) Math.ceil( (double) proc_end.b / allocUnit_size); 

               for (int i = first_allocUnit; i - first_allocUnit < last_allocUnit; i++) 
                  virtual_memory_bm.clear(i);

               // Imprimimos a nova configuração da memória virtual
               //print_virtual_memory();
            }
            
            int count = 0;
            for (int i = 0; i < pages_table.length; i++)
               if (pages_table[i].proc_pid != -1) count++;
            if (count > max) max = count;

            event = select_event (-1);
            tmin = event[0];
         }

         // Envelhecemos as páginas que estão presentes
         if (alg_pages == 2 || alg_pages == 3) 
            for (Page p : present_pages)
               if (time < pages_table[p.page].aging_time && pages_table[p.page].aging_time <= interval)
                  pages_table[p.page].r = false;

         // Atualizamos os contadores do LRU
         if(alg_pages == 4) {
            for(Page p : present_pages) {

               // Flipamos o bit mais alto do nosso contador
               if(pages_table[p.page].r)
                  pages_table[p.page].lru_counter += 128;

               // Shiftamos todo mundo por 1 bit
               pages_table[p.page].lru_counter >>>= 1;
            }
         }
         // Imprimimos o estado de cada uma das memórias
         System.out.println("TEMPO: " + (time + interval));
         System.out.println();
         print_bitmap();
         System.out.println();
         print_virtualPage();
         System.out.println();
         print_realPage();
         System.out.println(); 
         time += interval;
      }
      long total = 0;
      for (long d : total_time)
         total += d;
      double temp_gasto = (double) total / total_time.size();
      System.out.println("Page Faults: " + fault_count);
      System.out.println("Tempo médio gasto: " + temp_gasto);
   }

   // execute_command: recebe um vetor de string s e 
   //executa o respectivo comando
   public static void execute_command (String[] s) throws IOException { 
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
         System.out.println ("console: algoritmo de paginação numero " + alg_pages + " selecionado!");
         //sim.set_pageManagement(s[1]);
      }
      //Inicia simulador com intervalo
      else if (s[0].equals("executa")) {
         interval = Double.parseDouble(s[1]);
         System.out.println ("console: executando o simulador!");
         init_simulator();
         //sim.init();
      }
      else {
         System.out.println ("Comando INVÁLIDO!");
      }
   }

   public static void main (String[] args) throws IOException {
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