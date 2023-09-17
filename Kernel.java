import java.beans.Introspector;
import java.lang.Thread;
import java.io.*;
//import Page;
import java.util.*;

public class Kernel extends Thread
{
  // The number of virtual pages must be fixed at 63 due to
  // dependencies in the GUI
  private static int virtPageNum = 63;

  private String output = null;
  private static final String lineSeparator = 
  System.getProperty("line.separator");
  private String command_file;
  private String config_file;
  private ControlPanel controlPanel ;
  private Vector memVector = new Vector();
  private Vector instructVector = new Vector();
  private Vector segmentInstrVector = new Vector();
  private String status;
  private boolean doStdoutLog = false;
  private boolean doFileLog = false;
  public int runs;
  public int runcycles;
  public long block = (int) Math.pow(2,12);
  public static byte addressradix = 10;

  private Map<Integer, Long> asigSemgmen = new HashMap<>();
  /*La usaremos para almacenar y e esta misma forma recupperar valores */
  private Map<Integer, Long> segAsig = new HashMap<>();


  public void init( String commands , String config )  
  {
    
    File f = new File( commands );
    command_file = commands;
    config_file = config;
    String line;
    String tmp = null;
    String command = "";
    byte R = 0;
    byte M = 0;
    int i = 0;
    int j = 0;
    int id = 0;
    int physical = 0;
    int physical_count = 0;
    int inMemTime = 0;
    int lastTouchTime = 0;
    int map_count = 0;
    double power = 14;
    long high = 0;
    long low = 0;
    long addr = 0;
    long address_limit = (block * virtPageNum+1)-1;
  
    if ( config != null )
    {
      f = new File ( config );
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numpages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              virtPageNum = Common.s2i(st.nextToken()) - 1;
              if ( virtPageNum < 2 || virtPageNum > 63 )
              {
                System.out.println("MemoryManagement: numpages out of bounds.");
                System.exit(-1);
              }
              address_limit = (block * virtPageNum+1)-1;
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
      for (i = 0; i <= virtPageNum; i++) 
      {
        high = (block * (i + 1))-1;
        low = block * i;
        memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
      }
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 

        {
          if (line.startsWith("memset")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            while (st.hasMoreTokens()) 
            { 
              id = Common.s2i(st.nextToken());
              tmp = st.nextToken();
              if (tmp.startsWith("x")) 
              {
                physical = -1;
              } 
              else 
              {
                physical = Common.s2i(tmp);
              }
              if ((0 > id || id > virtPageNum) || (-1 > physical || physical > ((virtPageNum - 1) / 2)))
              {
                System.out.println("MemoryManagement: Invalid page value in " + config);
                System.exit(-1);
              }
              R = Common.s2b(st.nextToken());
              if (R < 0 || R > 1)
              {
                System.out.println("MemoryManagement: Invalid R value in " + config);
                System.exit(-1);
              }
              M = Common.s2b(st.nextToken());
              if (M < 0 || M > 1)
              {
                 System.out.println("MemoryManagement: Invalid M value in " + config);
                 System.exit(-1);
              }
              inMemTime = Common.s2i(st.nextToken());
              if (inMemTime < 0)
              {
                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                System.exit(-1);
              }
              lastTouchTime = Common.s2i(st.nextToken());
              if (lastTouchTime < 0)
              {
                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                System.exit(-1);
              }
              Page page = (Page) memVector.elementAt(id);
              page.physical = physical;
              page.R = R;
              page.M = M;
              page.inMemTime = inMemTime;
              page.lastTouchTime = lastTouchTime;
            }
          }
          if (line.startsWith("enable_logging")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              if ( st.nextToken().startsWith( "true" ) )
              {
                doStdoutLog = true;
              }              
            }
          }
          if (line.startsWith("log_file")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
            }
            if ( tmp.startsWith( "log_file" ) )
            {
              doFileLog = false;
              output = "tracefile";
            }              
            else
            {
              doFileLog = true;
              doStdoutLog = false;
              output = tmp;
            }
          }
          if (line.startsWith("pagesize")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              if ( tmp.startsWith( "power" ) )
              {
                power = (double) Integer.parseInt(st.nextToken());
                block = (int) Math.pow(2,power);
              }
              else
              {
                block = Long.parseLong(tmp,10);             
              }
              address_limit = (block * virtPageNum+1)-1;
            }
            if ( block < 64 || block > Math.pow(2,26))
            {
              System.out.println("MemoryManagement: pagesize is out of bounds");
              System.exit(-1);
            }
            for (i = 0; i <= virtPageNum; i++) 
            {
              Page page = (Page) memVector.elementAt(i);
              page.high = (block * (i + 1))-1;
              page.low = block * i;
            }
          }
          if (line.startsWith("addressradix")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              addressradix = Byte.parseByte(tmp);
              if ( addressradix < 0 || addressradix > 20 )
              {
                System.out.println("MemoryManagement: addressradix out of bounds.");
                System.exit(-1);
              }
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
    }
    f = new File ( commands );
    try 
    {
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      while ((line = in.readLine()) != null)
      {
        //System.out.println(line);
        if (line.startsWith("READ") || line.startsWith("WRITE"))
        {
          if (line.startsWith("READ"))
          {
            command = "READ";
          }
          if (line.startsWith("WRITE"))
          {
            command = "WRITE";
          }
          StringTokenizer st = new StringTokenizer(line);
          tmp = st.nextToken();
          tmp = st.nextToken();

          if (tmp.startsWith("random"))
          {
            instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
          } 
          else 
          { 
            if ( tmp.startsWith( "bin" ) )
            {
              addr = Long.parseLong(st.nextToken(),2);
            }
            else if ( tmp.startsWith( "oct" ) )
            {
              addr = Long.parseLong(st.nextToken(),8);
            }
            else if ( tmp.startsWith( "hex" ) )
            {
              addr = Long.parseLong(st.nextToken(),16);
            }
            else
            {
              addr = Long.parseLong(tmp);
            }
            if (0 > addr || addr > address_limit)
            {
              System.out.println("MemoryManagement: " + addr + ", Address out of range in " + commands);
              System.exit(-1);
            }
            instructVector.addElement(new Instruction(command,addr));
          }
        }
      }
      in.close();
    } catch (IOException e) { /* Handle exceptions */ }
    runcycles = instructVector.size();
    if ( runcycles < 1 )
    {
      System.out.println("MemoryManagement: no instructions present for execution.");
      System.exit(-1);
    }
    if ( doFileLog )
    {
      File trace = new File(output);
      trace.delete();
    }
    runs = 0;
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = (Page) memVector.elementAt(i);
      if ( page.physical != -1 )
      {
        map_count++;
      }
      for (j = 0; j < virtPageNum; j++) 
      {
        Page tmp_page = (Page) memVector.elementAt(j);
        if (tmp_page.physical == page.physical && page.physical >= 0)
        {
          physical_count++;
        }
      }
      if (physical_count > 1)
      {
        System.out.println("MemoryManagement: Duplicate physical page's in " + config);
        System.exit(-1);
      }
      physical_count = 0;
    }
    if ( map_count < ( virtPageNum +1 ) / 2 )
    {
      for (i = 0; i < virtPageNum; i++) 
      {
        Page page = (Page) memVector.elementAt(i);
        if ( page.physical == -1 && map_count < ( virtPageNum + 1 ) / 2 )
        {
          page.physical = i;
          map_count++;
        }
      }
    }
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = (Page) memVector.elementAt(i);
      if (page.physical == -1) 
      {
        controlPanel.removePhysicalPage( i );
      } 
      else
      {
        controlPanel.addPhysicalPage( i , page.physical );
      }
    }
    for (i = 0; i < instructVector.size(); i++) 
    {
      high = block * virtPageNum;
      Instruction instruct = ( Instruction ) instructVector.elementAt( i );
      if ( instruct.addr < 0 || instruct.addr > high )
      {
        System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
        System.exit(-1);
      }
    }
  }

  /*Este metodo se encarga de recibir las cadenas del archivo commands */

  public void segmentInstReader(String commands){
    System.out.println(commands);//Nos muestr contenido
    File f = new File(commands);
    String line = "";
    String command = "";
    //Vector instructions = new Vector();
    long addr1 = 0;
    long addr2 = 0;

    try {
      /*Se lee el archivo f el cual fue creado anteriormente */
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      /*Lee linea por linea del archivo  */
      while ((line = in.readLine()) != null) {
        /*Verifica como es que comienza la linea y le asigana un valor  */
        if (line.startsWith("READ") || line.startsWith("WRITE")){
          if(line.startsWith("READ")){
            command = "READ";
          }

          if(line.startsWith("WRITE")){
            command = "WRITE";
          }

          ////////////////////////////////////////////////
          /*Divide y almacena las partes en en arreglo */
          // split de string
          String[] spl = line.split(" ");

          /*Convierte las cadenas hexa en long y asi asignarlo a las rspectivas
           variables para seguir usandolos de forma hexa */
          addr1 = Long.parseLong(spl[2], 16);
          addr2 = Long.parseLong(spl[3], 16);

          /*Se comparan */
          if (addr1 > addr2){ // intercambio valores 
            long myTemp = 0;
            myTemp = addr1;
            addr1 = addr2;
            addr2 = myTemp;
          }
          segmentInstrVector.addElement(new Instruction(command, addr1, addr2));

          Instruction instr = new Instruction(command, addr1, addr2);

          
          //for(int i = 0; i<segmentInstrVector.size(); i++){
            //Instruction ins = (Instruction) segmentInstrVector.elementAt(i);
            //System.out.print("- " + ins.inst + " " + ins.addr1 + " " + ins.addr2 + "\n");
          //}

        }
      }
      in.close();
    } catch (IOException err){
      System.out.println("error");
    }
  }
  
  /*Recibe instruction, aqui se determinara en que segmento se encuentra 
   el rango de direcciones*/
  public String getDirectionResult(Instruction instr){

    /*16383 Valor en dec, representa tam de una hoja  */
    long pageSize = Long.parseLong("3fff", 16);

    System.out.println(Long.toHexString(pageSize));

    /*Aqui se define el limite de los segmmentos  */
    // Segmentos
    long origin_s1 = 0; // pagina inicial | 0
    long end_s1 = Long.parseLong("23fff", 16); // pagina final | 8
    long origin_s2 = Long.parseLong("24000", 16); // 9
    long end_s2 = Long.parseLong("37fff", 16); // 13
    long origin_s3 = Long.parseLong("38000", 16); // 14
    long end_s3 = Long.parseLong("5bfff", 16); // 22
    long origin_s4 = Long.parseLong("5c000", 16); //23
    long end_s4 = Long.parseLong("73fff", 16); // 28
    long origin_s5 = Long.parseLong("74000", 16); //29
    long end_s5 = Long.parseLong("7ffff", 16); //31

    // saber que paginas abarcan
    long origin_page = 0;
    long end_page = 0;

    // Obteniendo las posiciones del archivo commands


    // comparando posición (addr debe ser menor a addr2)
    /*Obtiene las direcciones y se imprime el tamaño del seg */
    long addr1 = instr.addr1;
    long addr2 = instr.addr2;

    System.out.println(addr1+" address to "+addr2 + " address");
    System.out.println("tamaño de pagina: "+pageSize);

    //origin_page = addr1 / pageSize; // numero de pagina (1-31)
    //end_page = addr2 / pageSize;
    origin_page = addr1;
    end_page = addr2;

    // saber en que segmento se encuentra

    System.out.println(
            "S1: " + Long.toHexString(origin_s1) + "-" + Long.toHexString(end_s1) + "\n"+
            "S2: " + Long.toHexString(origin_s2) + "-" + Long.toHexString(end_s2) + "\n"+
            "S3: " + Long.toHexString(origin_s3) + "-" + Long.toHexString(end_s3) + "\n"+
            "S4: " + Long.toHexString(origin_s4) + "-" + Long.toHexString(end_s4) + "\n"+
            "S5: " + Long.toHexString(origin_s5) + "-" + Long.toHexString(end_s5) + "\n"
    );

    String result = "";
    // saber en que segmento se localiza la selección de archivo commands
    if((origin_page >= origin_s1 && origin_page <= end_s1) && (end_page >= origin_s1 && end_page <= end_s1)){
      System.out.println("S1");
      result += "S1 ";
    } else if ((origin_page >= origin_s2 && origin_page <= end_s2) && (end_page >= origin_s2 && end_page <= end_s2)) {
      System.out.println("S2");
      result += "S2 ";
    } else if ((origin_page >= origin_s3 && origin_page <= end_s3) && (end_page >= origin_s3 && end_page <= end_s3)) {
      System.out.println("S3");
      result += "S3 ";
    } else if ((origin_page >= origin_s4 && origin_page <= end_s4) && (end_page >= origin_s4 && end_page <= end_s4)) {
      System.out.println("S4");
      result += "S4 ";
    } else if ((origin_page >= origin_s5 && origin_page <= end_s5) && (end_page >= origin_s5 && end_page <= end_s5)) {
      System.out.println("S5");
      result += "S5 ";
    } else {
      System.out.println("Error, abarca dos o más segmentos a la vez");
      result += "Error, abarca dos o más segmentos a la vez\n";
      return result;
    }

    long fromPage = (origin_page/pageSize==0)?0:(origin_page/pageSize)+1;
    long toPage = end_page/pageSize+1;
    System.out.println(fromPage + " to " + toPage + " page");
    result += "- "+ fromPage + " to " + toPage + " page \n";
    //result += Long.toHexString(addr1) + " address to " + Long.toHexString(addr2);
    return result;
  }

  /*BEST-FIT */
  /* */
  public String bestFit(Instruction instr) {
     long[] segmentInit;
     long[] segmentEnd;

    /*Aqui se definen las direcciones de incio -fin de los segmentos correspondientes*/
    // Límites de los segmentos
    segmentInit = new long[] {
      Long.parseLong("0",16),// S1
      Long.parseLong("24000", 16),  // S2
      Long.parseLong("38000", 16),  // S3
      Long.parseLong("5c000", 16),  // S4
      Long.parseLong("74000", 16)   // S5
    };

    segmentEnd = new long[] {
      Long.parseLong("23fff", 16),  // S1
      Long.parseLong("37fff", 16),  // S2
      Long.parseLong("5bfff", 16),  // S3
      Long.parseLong("73fff", 16),  // S4
      Long.parseLong("7ffff", 16)   // S5
    };

    

    // Direcciones de la instrucción
    long addr1 = instr.addr1;
    long addr2 = instr.addr2;

    // Calcula el tamaño de la instrucción
    /*Se btiene la diferencia entre estas 
     agrega 1 para que se de la ubiccion final de esta
     */
    long instrSize = addr2 - addr1 + 1;
    
   // inicia el índice del segmento como -1
   // este indica que no se a encontradp  algun segmento mejotr ajuste 
    int bestFitInd = -1;

    // Encuentra el mejor segmento que se ajuste al tamaño de la instrucción
    /*Aqui itera los seg disponibles, donde comparara tam de la instruc con el tam
     * de cada seg 
     */
    for (int i = 0; i < segmentInit.length; i++) {
        long segmentSize = segmentEnd[i] - segmentInit[i] + 1;

        // Si la instrucción cabe en el segmento y es dodne mejor se  ajusta hasta ahora
        //actualiza con el ind del segmento actu
        if (instrSize <= segmentSize && (bestFitInd == -1 || segmentSize < (segmentEnd[bestFitInd] - segmentInit[bestFitInd] + 1))) {
            bestFitInd = i;
        }
    }

    // Si se encontró un mejor segmento, devuelve su nombre (S1, s2, s3, s4, s5.)
    if (bestFitInd != -1) { /* Si es dif de -1 */
      /*Si encuenra seg adecuado regresa el nume del seg */
        return "Best-Fit: S" + (bestFitInd + 1);
    } else {
      /*No encuentra seg adecuado */
        return "Best-Fit: No se encontró un segmento adecuado para la instrucción.";
    }
    
  }
  
  /*Escanea la memoria desde el comienzo y elige el primer bloque 
  disponible que sea lo suficientemente grande. */
  public String firstFit(Instruction instr) {
        // Límites de los segmentos
    long[] segmentInit = {
      Long.parseLong("0", 16),     // S1
      Long.parseLong("24000", 16), // S2
      Long.parseLong("38000", 16), // S3
      Long.parseLong("5c000", 16), // S4
      Long.parseLong("74000", 16)  // S5
    };

    long[] segmentEnd = {
      Long.parseLong("23fff", 16), // S1
      Long.parseLong("37fff", 16), // S2
      Long.parseLong("5bfff", 16), // S3
      Long.parseLong("73fff", 16), // S4
      Long.parseLong("7ffff", 16)  // S5
    };

    // Direcciones de la instrucción
    long addr1 = instr.addr1;
    long addr2 = instr.addr2;

    // Calcula el tamaño de la instrucción
    /*Se btiene la diferencia entre estas 
    agrega 1 para que se de la ubiccion final de esta
    */
    long instrSize = addr2 - addr1 + 1;

    int firstIndice = -1; // Índice del segmento que se ajusta primero

    // Busca el primer segmento adecuado desde el comienzo
    /*itera atraves de segmentis definidos y se verfica que cada segemntio este 
    * ocupado (comptobando en el mapeo ) asigancionesen los segmen
    */
    for (int i = 0; i < segmentInit.length; i++) {
      long segmentStart = segmentInit[i];
      long segmentEnds = segmentEnd[i];

      // Verifica si el segmento está ocupado
      /*Idenficica por ind el segmenti->si esta registrado en el map 
      * revisa si no ha sido asigando
      * Revisa si entra en el segmento sin pasarse de su limite 
      */
      /*Si es menos igual al tamaño del segemnto(diferencis entre limites ) */
      if (!asigSemgmen.containsKey(i) && instrSize <= (segmentEnds - segmentStart + 1)) {
        firstIndice = i;
        /*agrega un elemento al map, i=indi, calcula la direccion de la aigamcion
        * del seg*/
        asigSemgmen.put(i, segmentStart + instrSize); // Registra la asignación del segmento
        break; // Se encontró el primer segmento adecuado, salir del bucle
      }
    }

    if (firstIndice != -1) {
        return "First-Fit: S" + (firstIndice + 1);
    } else {
        return "First-Fit: No se encontró un segmento adecuado para la instrucción.";
    }
  }

  /*Next-Fit Comienza a escanear la memoria desde la localización del último bloque de memoria
  alojado y elige el siguiente bloque disponible lo suficientemente grande.*/
  public String nextFit(Instruction instr) {
    /*Se encarga del ultimo segm de memoria asignada */
    int ultSegAsig = -1;
    // Límites de los segmentos

    long[] segmentInit = {
        Long.parseLong("0", 16),     // S1
        Long.parseLong("24000", 16), // S2
        Long.parseLong("38000", 16), // S3
        Long.parseLong("5c000", 16), // S4
        Long.parseLong("74000", 16)  // S5
    };

    long[] segmentEnd = {
        Long.parseLong("23fff", 16), // S1
        Long.parseLong("37fff", 16), // S2
        Long.parseLong("5bfff", 16), // S3
        Long.parseLong("73fff", 16), // S4
        Long.parseLong("7ffff", 16)  // S5
    };

    // Direcciones de la instrucción
    long addr1 = instr.addr1;
    long addr2 = instr.addr2;

    // Calcula el tamaño de la instrucción
    long instrSize = addr2 - addr1 + 1;

    int nextfitSegmIn = -1; // Índice  seg, el cual almacena idicara si es adecuaso

    // Comienza a escanear el último bloque asignado
    /*?= este sirve como evaluador de una condicion, si  es vdd
    entonces primero revisa que no tiene asigando ningun segmento  == -1 y guarda 0 en indi
    Si es falso inicia la segundsa parte, el cual indica que ya tiene asigandoun segmento*/
    int indI = (ultSegAsig == -1) ? 0 : (ultSegAsig + 1);

    // Busca el siguiente segmento adecuado
    /*el ciclo recorre los segmentos */
    for (int i = indI; i < segmentInit.length; i++) {
        long segmentStart = segmentInit[i];
        long segmentEnds = segmentEnd[i];

        /*  Verifica si el segmento no está ocupado
        Si cumole dicha condicio, el segmento se marca como asignado y
        e actualiza el indicide de este segmen*/
        if (!segAsig.containsKey(i) && instrSize <= (segmentEnds - segmentStart + 1)) {
            nextfitSegmIn = i;
            segAsig.put(i, segmentStart + instrSize); // Registra la asignación del segmento
            ultSegAsig = i; // Actualiza el último segmento asignado
            break; // Se encontró el siguiente segmento adecuado, salir del bucle
        }
    }
    /*Si es diferente de -1 = segmento adecuato  devuelve el segemnto correspondiemte   */
    if (nextfitSegmIn != -1) {
        return "Next-Fit: S" + (nextfitSegmIn + 1);
    } else {
        return "Next-Fit: No se encontró un segmento adecuado para la instrucción.";
    }
  }



  public void setControlPanel(ControlPanel newControlPanel) 
  {
    controlPanel = newControlPanel ;
  }

  public void getPage(int pageNum) 
  {
    Page page = ( Page ) memVector.elementAt( pageNum );
    controlPanel.paintPage( page );
  }

  private void printLogFile(String message)
  {
    String line;
    String temp = "";

    File trace = new File(output);
    if (trace.exists()) 
    {
      try 
      {
        DataInputStream in = new DataInputStream( new FileInputStream( output ) );
        while ((line = in.readLine()) != null) {
          temp = temp + line + lineSeparator;
        }
        in.close();
      }
      catch ( IOException e ) 
      {
        /* Do nothing */
      }
    }
    try 
    {
      PrintStream out = new PrintStream( new FileOutputStream( output ) );
      out.print( temp );
      out.print( message );
      out.close();
    } 
    catch (IOException e) 
    {
      /* Do nothing */ 
    }
  }

  public void run()
  {
    step();
    while (runs != runcycles) 
    {
      try 
      {
        Thread.sleep(2000);
      } 
      catch(InterruptedException e) 
      {  
        /* Do nothing */ 
      }
      step();
    }  
  }

  public void step()
  {
    int i = 0;

    Instruction instruct = ( Instruction ) instructVector.elementAt( runs );
    controlPanel.instructionValueLabel.setText( instruct.inst );

    // get address
    controlPanel.addressValueLabel.setText( Long.toString( instruct.addr , addressradix ) );

    getPage( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
    if ( controlPanel.pageFaultValueLabel.getText() == "YES" ) 
    {
      controlPanel.pageFaultValueLabel.setText( "NO" );
    }
    if ( instruct.inst.startsWith( "READ" ) ) 
    {
      Page page = ( Page ) memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) , controlPanel );
        controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.R = 1;
        page.lastTouchTime = 0;   
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
      }
    }
    if ( instruct.inst.startsWith( "WRITE" ) ) 
    {
      Page page = ( Page ) memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
           System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) , controlPanel );          controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.M = 1;
        page.lastTouchTime = 0;
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
      }
    }
    for ( i = 0; i < virtPageNum; i++ ) 
    {
      Page page = ( Page ) memVector.elementAt( i );
      if ( page.R == 1 && page.lastTouchTime == 10 ) 
      {
        page.R = 0;
      }
      if ( page.physical != -1 ) 
      {
        page.inMemTime = page.inMemTime + 10;
        page.lastTouchTime = page.lastTouchTime + 10;
      }
    }

    /////////////////////////////////////////////////////////

    Instruction ins = (Instruction) segmentInstrVector.elementAt(runs);
    controlPanel.segmentState.setText(this.getDirectionResult(ins));

    // Llama a la función bestFit y muestra el resultado
    String result = bestFit(ins);
    System.out.println(result);
    /*Llama a la funcion First-Fit */
    result = firstFit(ins);
    System.out.println(result);
    /*La funcionllama a next-fit , y muestra los resultados optenidos */
    result = nextFit(ins);
    System.out.println(result);

    System.out.print("- " + ins.inst + " " + Long.toHexString(ins.addr1) + " " + Long.toHexString(ins.addr2) + "\n");

    /////////////////////////////////////////////////////////

    runs++;
    controlPanel.timeValueLabel.setText( Integer.toString( runs*10 ) + " (ns)" );
  }

  public void reset() {
    memVector.removeAllElements();
    instructVector.removeAllElements();
    controlPanel.statusValueLabel.setText( "STOP" ) ;
    controlPanel.timeValueLabel.setText( "0" ) ;
    controlPanel.instructionValueLabel.setText( "NONE" ) ;
    controlPanel.addressValueLabel.setText( "NULL" ) ;
    controlPanel.pageFaultValueLabel.setText( "NO" ) ;
    controlPanel.virtualPageValueLabel.setText( "x" ) ;
    controlPanel.physicalPageValueLabel.setText( "0" ) ;
    controlPanel.RValueLabel.setText( "0" ) ;
    controlPanel.MValueLabel.setText( "0" ) ;
    controlPanel.inMemTimeValueLabel.setText( "0" ) ;
    controlPanel.lastTouchTimeValueLabel.setText( "0" ) ;
    controlPanel.lowValueLabel.setText( "0" ) ;
    controlPanel.highValueLabel.setText( "0" ) ;
     controlPanel.segmentState.setText("              0");
    init( command_file , config_file );
  }
}
