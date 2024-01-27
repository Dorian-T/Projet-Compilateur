package Asm;

public class Com extends Instruction {
    public Com(String comment) {
        super("", comment);
    }

    @Override
    public String toString() {
        return "#" + name + "\n";
    }
}
