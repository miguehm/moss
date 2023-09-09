public class Instruction 
{
  public String inst;
  public long addr;
  public long addr1;
  public long addr2;

  public Instruction( String inst, long addr ) 
  {
    this.inst = inst;
    this.addr = addr;
  }

  public Instruction(String inst, long addr1, long addr2){
    this.inst = inst;
    this.addr = 0;
    this.addr1 = addr1;
    this.addr2 = addr2;
  }

}
