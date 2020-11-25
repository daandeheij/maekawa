import java.util.ArrayList;
import java.util.List;

public class MaekawaObserver {

    public List<MaekawaProcess> processes;
    
    public MaekawaObserver() {
        this.processes = new ArrayList<MaekawaProcess>();
    }

    public void register(MaekawaProcess process){
        processes.add(process);
    }

    public void inform() {
        if (deadlock()) System.out.println("DEADLOCK");
    }

    private boolean deadlock() {
        for (MaekawaProcess process : processes) {
            if (containsWaitingCycle(process, null)) return true;
        }
        return false;
    }
    
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
