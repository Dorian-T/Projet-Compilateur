package Graph;
import java.util.*;

public class Variables {
    private GrapheDeControle graphe;
    private List<String> instructions;
    private HashMap<Integer, Set<Integer>> generatedVariables;
    private HashMap<Integer, Set<Integer>> killedVariables;
    private HashMap<Integer, Set<Integer>> lvEntry;
    private HashMap<Integer, Set<Integer>> lvExit;

    public Variables(GrapheDeControle graphe, List<String> instructions) {
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
        for (int i = 0; i < instructions.size(); i++) {
            String instr = instructions.get(i);
            Set<Integer> genVars = new HashSet<>();
            Set<Integer> killVars = new HashSet<>();
            // Afficher l'instruction pour le débogage
            String[] parts = instr.split("\\s+");
            if (parts.length < 2) continue; // Skip labels and empty lines

            int startIndex = parts[0].endsWith(":") ? 1 : 0; // Skip label if present
            String opcode = parts[startIndex];

            switch (opcode) {
                case "LD":
                    // Format: LD Rdest Rsrc
                    genVars.add(extractRegister(parts[startIndex + 2])); // Ajout du registre source
                    killVars.add(extractRegister(parts[startIndex + 1])); // Ajout du registre destination
                    break;
                case "XOR":
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                case "MOD":
                case "AND":
                case "OR":
                case "SL":
                case "SR":
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    genVars.add(extractRegister(parts[startIndex + 3]));
                    break;
                case "ADDi":
                case "SUBi":
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                case "JMP":
                case "CALL":
                    break;
                case "JSUP":
                case "JINF":
                case "JEQU":
                case "JNEQ":
                case "JIEQ":
                case "JSEQ":
                    genVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                case "ST":
                    killVars.add(extractRegister(parts[startIndex + 1])); // tue address
                    genVars.add(extractRegister(parts[startIndex + 2])); // génère src
                    break;
                case "RET":
                    break;
                default:
                    break;
            }
            generatedVariables.put(i, genVars);
            killedVariables.put(i, killVars);
        }
    }

    public void calculateLVEntryExit() {
        boolean changed;
        do {
            changed = false;
            for (int index = instructions.size() - 1; index >= 0; index--) {
                Set<Integer> currentLVExit = new HashSet<>();
                List<Integer> outNeighbors = graphe.getOutNeighbors(index);
    
                if (outNeighbors != null) {
                    for (int neighborIndex : outNeighbors) {
                        currentLVExit.addAll(lvEntry.getOrDefault(neighborIndex, new HashSet<>()));
                    }
                }
    
                Set<Integer> currentLVEntry = new HashSet<>(currentLVExit);
                currentLVEntry.removeAll(killedVariables.getOrDefault(index, new HashSet<>()));
                currentLVEntry.addAll(generatedVariables.getOrDefault(index, new HashSet<>()));
    
                if (!currentLVExit.equals(lvExit.get(index))) {
                    lvExit.put(index, currentLVExit);
                    changed = true;
                }
    
                if (!currentLVEntry.equals(lvEntry.get(index))) {
                    lvEntry.put(index, currentLVEntry);
                    changed = true;
                }
            }
        } while (changed);
    }
    

    private int extractRegister(String reg) {
        if (reg.matches("R\\d+")) {
            return Integer.parseInt(reg.substring(1));
        } else {
            return -1; // Return -1 if it's not a register
        }
    }

    public Set<Integer> getGeneratedVariables(int index) {
        return generatedVariables.getOrDefault(index, new HashSet<>());
    }

    public Set<Integer> getKilledVariables(int index) {
        return killedVariables.getOrDefault(index, new HashSet<>());
    }

    public Set<Integer> getLVEntry(int index) {
        return lvEntry.getOrDefault(index, new HashSet<>());
    }

    public Set<Integer> getLVExit(int index) {
        return lvExit.getOrDefault(index, new HashSet<>());
    }
    public boolean isUsedOrDefinedElsewhere(Integer reg, String excludingInstr) {
        for (String instr : instructions) {
            if (!instr.equals(excludingInstr)) {
                Set<Integer> genVars = generatedVariables.getOrDefault(instr, new HashSet<>());
                Set<Integer> killVars = killedVariables.getOrDefault(instr, new HashSet<>());

                if (genVars.contains(reg) || killVars.contains(reg)) {
                    return true; // Le registre est utilisé ou défini dans une autre instruction
                }
            }
        }
        return false; // Le registre n'est pas utilisé ou défini ailleurs
    }
}