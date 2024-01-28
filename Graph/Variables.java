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
            if (instr.trim().startsWith("#")) continue;
            Set<Integer> genVars = new HashSet<>();
            Set<Integer> killVars = new HashSet<>();
            String[] parts = instr.split("\\s+");
            if (parts.length < 2) continue;
    
            int startIndex = parts[0].endsWith(":") ? 1 : 0;
            String opcode = parts[startIndex];
    
            switch (opcode) {
                case "LD":
                    genVars.add(extractRegister(parts[startIndex + 1])); 
                    killVars.add(extractRegister(parts[startIndex + 2])); 
                    break;
                case "ST":
                    killVars.add(extractRegister(parts[startIndex + 1])); 
                    genVars.add(extractRegister(parts[startIndex + 2])); 
                    break;
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                case "MOD":
                case "XOR":
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
                case "RET":
                    break;
                default:
                    break;
            }
    
            generatedVariables.put(instr, genVars);
            killedVariables.put(instr, killVars);
        }
    }

    public void calculateLVEntryExit() {
        // Initialisation de LVExit avec les variables générées
        for (String instr : instructions) {
            Set<Integer> initialLVExit = new HashSet<>(generatedVariables.getOrDefault(instr, new HashSet<>()));
            lvExit.put(instr, initialLVExit);
        }

        boolean changed;
        do {
            changed = false;
            for (String instr : instructions) {
                if (instr.trim().startsWith("#")) continue;
                Set<Integer> newLVEntry = new HashSet<>(lvExit.getOrDefault(instr, new HashSet<>()));
                newLVEntry.removeAll(killedVariables.getOrDefault(instr, new HashSet<>()));
                newLVEntry.addAll(generatedVariables.getOrDefault(instr, new HashSet<>()));

                if (!newLVEntry.equals(lvEntry.get(instr))) {
                    lvEntry.put(instr, newLVEntry);
                    changed = true;
                }

                Set<Integer> newLVExit = new HashSet<>();
                for (String succ : graphe.getOutNeighbors(instr)) {
                    newLVExit.addAll(lvEntry.getOrDefault(succ, new HashSet<>()));
                }

                if (!newLVExit.equals(lvExit.get(instr))) {
                    lvExit.put(instr, newLVExit);
                    changed = true;
                }
            }
        } while (changed);
    }

    private int extractRegister(String reg) {
        if (reg.matches("R\\d+")) {
            return Integer.parseInt(reg.substring(1));
        } else {
            return -1; 
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
    public boolean isUsedOrDefinedElsewhere(Integer reg, String excludingInstr) {
        for (String instr : instructions) {
            if (!instr.equals(excludingInstr)) {
                Set<Integer> genVars = generatedVariables.getOrDefault(instr, new HashSet<>());
                Set<Integer> killVars = killedVariables.getOrDefault(instr, new HashSet<>());

                if (genVars.contains(reg) || killVars.contains(reg)) {
                    return true; 
                }
            }
        }
        return false;
    }
}
