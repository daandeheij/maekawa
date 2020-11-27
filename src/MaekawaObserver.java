import java.util.ArrayList;
import java.util.List;

public class MaekawaObserver {

    public int numberOfProcesses;
    public List<MaekawaProcess> processes;
    
    public MaekawaObserver(int numberOfProcesses) {
        this.numberOfProcesses = numberOfProcesses;
        this.processes = new ArrayList<MaekawaProcess>();
    }

    /**
     * Adda a process to list that observer observes
     * @param process to be observed
     */
    public void register(MaekawaProcess process){
        processes.add(process);
    }

    /**
     * inform observer that waiting status of process has changed
     */
    public void inform() {
        if (processes.size() < numberOfProcesses) return;
        if (deadlock()) System.out.println("DEADLOCK");
    }

    /**
     * Determines whether deadlock is present within the system.
     * @return True if deadlock is present, false otherwise.
     */
    private boolean deadlock() {
        for (MaekawaProcess process : processes) {
            if (containsWaitingCycle(process, null)) return true;
        }
        return false;
    }
    
    /**
     * Checks whether the given root process is waiting for itself in a wait cycle.
     * @param root The root process.
     * @param process The waiting process.
     * @return True if the root process is waiting for itself, false otherwise.
     */
    private boolean containsWaitingCycle(MaekawaProcess root, MaekawaProcess process){
        if (root == process) return true;
        if (process == null) process = root;
        if (!process.waiting) return false;
        
        for (Integer resourceId : process.requestSet){
            if (!process.grantSet.contains(resourceId)) {
                MaekawaProcess resource = processes.get(resourceId);
                if (containsWaitingCycle(root, resource)) return true;
            }
        }
        return false;
    }
    
}
