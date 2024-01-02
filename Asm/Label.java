package Asm;

public class Label extends Instruction {
    public Label(String label) {
        super(label, "");
    }

    @Override
    public String toString() {
        return this.label + ":\n";
    }
}

