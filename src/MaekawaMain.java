import java.net.MalformedURLException;
import java.rmi.*;
import java.util.*;

public class MaekawaMain {
    public static void main(String[] args) {
        runThreeProcesses();
        // runManyProcesses();
    }
    
    /**
     * Runs the system with three processes.
     */
    public static void runThreeProcesses(){
        runRandomProcesses(3);
    }

    /**
     * Runs the system with many processes.
     */
    public static void runManyProcesses(){
        runRandomProcesses(8);
    }

    /**
     * Runs Maekawa's mutual-exclusion algorithm for the given number of random processes.
     * @param numberOfProcesses The number of processes in the system.
     */
    public static void runRandomProcesses(int numberOfProcesses){
        try {
            MaekawaProcess[] processes = new MaekawaProcess[numberOfProcesses];
            List<Set<Integer>> requestSets = generateRequestSets(numberOfProcesses, 0.1);
            MaekawaObserver observer = new MaekawaObserver();

            for (int i = 0; i < numberOfProcesses; i++) {
                int offset = (int) (5000 * Math.random());
                int period = 1000 + (int) (5000 * Math.random());
                int duration = 100 + (int) (100 * Math.random());
                processes[i] = new MaekawaProcess(i, numberOfProcesses, requestSets.get(i), offset, period, duration, observer);
            }
            
            runProcesses(processes);
        }
        catch (RemoteException e){
            e.printStackTrace();
        } 
    }

    /**
     * Runs Maekawa's mutual-exclusion algorithm for the given number of processes.
     * @param processes The array of processes of to run.
     */
    public static void runProcesses(MaekawaProcess[] processes) {
        try{

            java.rmi.registry.LocateRegistry.createRegistry(1099);
            for (int i = 0; i < processes.length; i++) {
                Naming.bind("rmi://localhost:1099/" + String.valueOf(i), processes[i]);
            }

            for (int i = 0; i < processes.length; i++) {
                Thread thread = new Thread(processes[i]);
                thread.start();
            }
        }
        catch (MalformedURLException | RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates random request sets for the given number of processes with the given add probability.
     * @param numberOfProcesses The number of processes in the system.
     * @param addProbability The probability with which a process gets added to a request set.
     * @return A list of request sets.
     */
    public static List<Set<Integer>> generateRequestSets(int numberOfProcesses, double addProbability){
        List<Set<Integer>> requestSets = new ArrayList<Set<Integer>>();
        Random random = new Random();

        // Generate random selection of resources
        for (int process = 0; process < numberOfProcesses; process++) {
            Set<Integer> requestSet= new HashSet<Integer>();
            
            // generate two random process in order to guarantee >=2 processes in request set
            int firstRandomProcess = process;
            while (firstRandomProcess == process) {
                firstRandomProcess = random.nextInt(numberOfProcesses);
            }
            int secondRandomProcess = process;
            while (secondRandomProcess == firstRandomProcess || secondRandomProcess == process) {
                secondRandomProcess = random.nextInt(numberOfProcesses);
            }
            
            // Add the random process ID's to the request set of the process.
            requestSet.add(firstRandomProcess);
            requestSet.add(secondRandomProcess);

            requestSets.add(requestSet);

            // Add each resource to resource set with probability probabilitAddTRequestSet
            for (int resource = 0; resource < numberOfProcesses; resource++) {
                boolean add = Math.random() < addProbability;
                if (process != resource && add) requestSets.get(process).add(resource);
            }
        }

        // Add one resource of each previous process in order to guarantee non-empty intersection
        for (int process = 1; process < numberOfProcesses; process++) {
            for (int processAddFrom = 0; processAddFrom < process; processAddFrom++) {
                Set<Integer> requestSetFrom = requestSets.get(processAddFrom);
                Set<Integer> requestSetTo = requestSets.get(process);
                int size = requestSetFrom.size();
                int randomProcess = process;

                // Ensure that the random resource is not equal to this current process
                while (randomProcess == process) {
                    int randomIndex = random.nextInt(size);
                    randomProcess = (int) requestSetFrom.toArray()[randomIndex];
                }

                requestSetTo.add(randomProcess);    
            }
        }
        return requestSets;
    }
}
