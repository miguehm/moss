import java.io.*;

public class Segment{
    private long origin;
    private long end;
    private long size;
    private Instruction actualInstruction;
    private String name;
    public boolean touched;

    public Segment(String origin, String end, String name){
        this.origin = Long.parseLong(origin, 16);
        this.end = Long.parseLong(end, 16);
        this.name = name;
        this.size = this.end - this.origin;
        this.touched = false;
    }

    public long getOrigin() {
        return this.origin;
    }

    public void setOrigin(long origin) {
        this.origin = origin;
    }

    public long getEnd() {
        return this.end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getSize() {
        return this.size;
    }

    public Instruction getActualInstruction() {
        return this.actualInstruction;
    }

    public String setActualInstruction(Instruction newInstruction) {

        String message = "";

        if(this.actualInstruction == null){
            message = this.name + " | " +
                "Asignado a "+Long.toHexString(newInstruction.addr1)+
                " - "+
                Long.toHexString(newInstruction.addr2);
            this.touched = true;
        } else {
            message = this.name+
                " | "+
                Long.toHexString(this.actualInstruction.addr1)+
                "-"+
                Long.toHexString(this.actualInstruction.addr2)+
                " <-> "+
                Long.toHexString(newInstruction.addr1)+
                "-"+
                Long.toHexString(newInstruction.addr2);
        }
        
        this.actualInstruction = newInstruction;

        return message;

    }
    
}
