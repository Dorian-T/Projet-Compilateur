package Graph;
import java.util.*;

public class Variables {
    private GrapheDeControle<String> graphe;
    private List<String> instructions;
    private HashMap<String, Set<Integer>> generatedVariables;
    private HashMap<String, Set<Integer>> killedVariables;
    private HashMap<String, Set<Integer>> lvEntry;
    private HashMap<String, Set<Integer>> lvExit;

    public Variables(GrapheDeControle<String> graphe, List<String> instructions) {
        this.graphe = graphe;
        this.instructions = instructions;
        this.generatedVariables = new HashMap<>();
        this.killedVariables = new HashMap<>();
        this.lvEntry = new HashMap<>();
        this.lvExit = new HashMap<>();
        calculateVariables();
        calculateLVEntryExit();
    }

    private void calculateVariables() {
        for (String instr : instructions) {
            Set<Integer> genVars = new HashSet<>();
            Set<Integer> killVars = new HashSet<>();
    
            String[] parts = instr.split("\\s+");
            if (parts.length < 2) continue; // Skip labels and empty lines
    
            // Traitement des labels
            int startIndex = 0;
            if (parts[0].endsWith(":")) {
                startIndex = 1; // Si la ligne commence par une étiquette, sautez-la.
            }
    
            String opcode = parts[startIndex];
            switch (opcode) {
                case "XOR":
                case "ADD":
                case "SUB":
                    // Format: OPCODE Rdest Rsrc1 Rsrc2
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    genVars.add(extractRegister(parts[startIndex + 3]));
                    break;
                case "ADDi":
                case "SUBi":
                    // Format: OPCODEi Rdest Rsrc Imm
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                case "JSUP":
                case "JEQU":
                case "JINF":
                case "JNEQ":
                    // Format: OPCODE Rsrc1 Rsrc2 Label
                    genVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                // Ajoutez d'autres cas au besoin
            }
    
            generatedVariables.put(instr, genVars);
            killedVariables.put(instr, killVars);
        }
    }

    void calculateLVEntryExit() {
        for (String instr : instructions) {
            lvEntry.put(instr, new HashSet<>(generatedVariables.getOrDefault(instr, new HashSet<>())));
        }
    
        for (String instr : instructions) {
            Set<Integer> initialLVExit = new HashSet<>();
            for (String succ : graphe.getOutNeighbors(instr)) {
                initialLVExit.addAll(lvEntry.getOrDefault(succ, new HashSet<>()));
            }
            lvExit.put(instr, initialLVExit);
        }

    }
    
    
    
    
    private int extractRegister(String reg) {
        if (reg.matches("R\\d+")) {
            return Integer.parseInt(reg.substring(1));
        } else {
            return -1; // Retourne -1 si ce n'est pas un registre
        }
    }
    

    public Set<Integer> getGeneratedVariables(String instr) {
        return generatedVariables.getOrDefault(instr, new HashSet<>());
    }

    public Set<Integer> getKilledVariables(String instr) {
        return killedVariables.getOrDefault(instr, new HashSet<>());
    }
    
    public Set<Integer> getLVEntry(String instr) {
        return lvEntry.getOrDefault(instr, new HashSet<>());
    }

    public Set<Integer> getLVExit(String instr) {
        return lvExit.getOrDefault(instr, new HashSet<>());
    }
}