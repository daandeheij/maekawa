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

    public static void runDeadlockProcesses(){
        try {
            MaekawaProcess[] processes = new MaekawaProcess[3];
            List<Set<Integer>> requestSets = new ArrayList<Set<Integer>>();
            MaekawaObserver observer = new MaekawaObserver();
        
            Integer[] requestArray0 = {0, 1, 2}; 
            Integer[] requestArray1 = {1, 4, 6}; 
            Integer[] requestArray2 = {2, 3, 4}; 
            Integer[] requestArray3 = {0, 3, 6}; 
            Integer[] requestArray4 = {0, 4, 5}; 
            Integer[] requestArray5 = {1, 3, 5}; 
            Integer[] requestArray6 = {2, 5, 6}; 

            requestSets.add(new HashSet<>(Arrays.asList(requestArray0)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray1)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray2)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray3)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray4)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray5)));
            requestSets.add(new HashSet<>(Arrays.asList(requestArray6)));

            processes[0] = new MaekawaProcess(0, 7, requestSets.get(0), 3000, 8000, 1000, observer);
            processes[1] = new MaekawaProcess(1, 7, requestSets.get(1), 40000, 8000, 1000, observer);
            processes[2] = new MaekawaProcess(2, 7, requestSets.get(2), 3000, 8000, 1000, observer);
            processes[3] = new MaekawaProcess(3, 7, requestSets.get(3), 40000, 8000, 1000, observer);
            processes[4] = new MaekawaProcess(4, 7, requestSets.get(4), 3000, 8000, 1000, observer);
            processes[5] = new MaekawaProcess(5, 7, requestSets.get(5), 40000, 8000, 1000, observer);
            processes[6] = new MaekawaProcess(6, 7, requestSets.get(6), 40000, 8000, 1000, observer);

            runProcesses(processes);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the distributed system for the total ordering of broadcast messages.
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
