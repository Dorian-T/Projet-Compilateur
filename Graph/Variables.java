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
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    genVars.add(extractRegister(parts[startIndex + 3]));
                    break;
                case "SUB":
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    genVars.add(extractRegister(parts[startIndex + 3]));
                    break;
                case "ADDi":
                case "SUBi":
                    killVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                case "JSUP":
                case "JEQU":
                case "JINF":
                case "JNEQ":
                    genVars.add(extractRegister(parts[startIndex + 1]));
                    genVars.add(extractRegister(parts[startIndex + 2]));
                    break;
                // Ajoutez d'autres cas au besoin
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
            return -1; // Return -1 if it's not a register
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
                    return true; // Le registre est utilisé ou défini dans une autre instruction
                }
            }
        }
        return false; // Le registre n'est pas utilisé ou défini ailleurs
    }
}
