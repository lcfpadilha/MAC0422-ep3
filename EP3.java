import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

public class EP3 {
   static BitSet virtual_memory_bm; //bitmap da memoria virtual (usado para alocaçao)
   static BitSet real_memory_bm; //bitmap da memoria real (usado para alocaçao)
   static int[][] table_pages;
   static LinkedList<Process> next_process; //processos que serão executados
   static LinkedList<Process> finished_process; //processos que estão em execução
   static LinkedList<Page> next_pages; //próximas páginas a serem acessadas
   static int alg_space, alg_pages;
   static double interval;

   public static void set_traceFile (String f_name) {
      int virtual_size, real_size, allocUnit_size, page_size;
      
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
         table_pages = new int[virtual_size / page_size][3]; //TODO: Verificar oque precisa na tabela de pagina

         // Inicialização do bitmap da memoria real
         virtual_memory_bm = new BitSet(real_size / allocUnit_size);
         virtual_memory_bm.clear();

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
            next_process.add(new Process(t0, name, tf, b, p_pages));
         }
      // Se houve erro
      } catch (FileNotFoundException fnfe) {
        System.out.println("ERRO:" + fnfe);
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

      while (!next_process.isEmpty() || !next_pages.isEmpty() || !finished_process.isEmpty()) {
         int tmin = -1;
         int event[];

         // Seleciona o próximo evento (aquele com tempo menor)
         event = select_event (tmin);
         tmin = event[0];

         while (tmin != -1 && tmin < time + interval) {
            if (event[1] == 1) {
               Process proc_next = next_process.removeFirst();
               // Aloca espaço livre pra ele
               while (!proc_next.pages.isEmpty())
                  next_pages.add(proc_next.pages.removeFirst());
            }
            else if (event[1] == 2) {
               Page p = next_pages.removeFirst();
               // Acessa a pagina

            }
            else if (event[1] == 3) // Remove o espaco que ele ocupou;
            
            event = select_event (-1);
            tmin = event[0];
            System.out.println (tmin + " " + time + " " + interval);
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