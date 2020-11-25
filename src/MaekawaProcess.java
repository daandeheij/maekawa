import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class MaekawaProcess extends UnicastRemoteObject implements MaekawaProcessRMI, Runnable {

    private static final long serialVersionUID = 1L;
    public int processId;
    public int[] clock;
    public Queue<MaekawaMessage> receivedRequests;
    public Set<Integer> requestSet;
    public int numberOfCriticalSections;
    public Set<Integer> grantSet;
    public boolean waiting;
    public boolean granted;
    public MaekawaMessage currentGrantMessage;
    public MaekawaObserver observer;

    protected MaekawaProcess(int processId, int numberOfProcesses, Set<Integer> requestSet, int numberOfCriticalSections, MaekawaObserver observer) throws RemoteException {
        this.processId = processId;
        this.clock = new int[numberOfProcesses];
        this.receivedRequests = new PriorityQueue<MaekawaMessage>();
        this.requestSet = requestSet;
        this.numberOfCriticalSections = numberOfCriticalSections;
        this.grantSet = new HashSet<Integer>();
        this.observer = observer;
        this.observer.register(this);
        this.waiting = false;
    }

    @Override
    public void run() {
        for (int i = 0; i < numberOfCriticalSections; i++){
            randomDelay();
            sendRequests();
        }
    }

    public void randomDelay(){
        try {
            int randomDelay = (int) (5000 * Math.random());
            Thread.sleep(randomDelay);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void criticalSection() {
        System.out.println("Process " + processId + " entering critical section with request set: " + requestSet.toString());
        randomDelay();
        System.out.println("Process " + processId + " leaving critical section with request set: " + requestSet.toString());
    }

    /**
     * Increments this process' clock.
     */
    public int[] incrementClock() {
        this.clock[processId]++;
        return this.clock.clone();
    }

    /**
     * Updates this process' clock bases on another clock.
     * 
     * @param otherClock The other clock to consider in the update.
     */
    public void updateClock(int[] otherClock) {
        for (int i = 0; i < clock.length; i++) {
            int time = clock[i];
            int otherTime = otherClock[i];
            if (otherTime > time){
                clock[i] = otherTime;
            }
        }
    }

    public void sendRequests() {
        int[] timestamp = incrementClock();
        waiting = true;
        observer.inform();
        for (int resourceId : requestSet) {
            sendMessage(resourceId, "REQUEST", timestamp);
        }
    }

    public void sendGrant(int receiverId) {
        int[] timestamp = incrementClock();
        sendMessage(receiverId, "GRANT", timestamp);
    }

    public void sendReleases() {
        grantSet.clear();
        int[] timestamp = incrementClock();

        for (int resourceId : requestSet) {
            sendMessage(resourceId, "RELEASE", timestamp);
        }
    }
    

    public void sendMessage(int receiverId, String messageType, int[] timestamp){
        try {
            MaekawaProcessRMI process = (MaekawaProcessRMI) Naming.lookup("rmi://localhost:1099/" + String.valueOf(receiverId));
            System.out.println("process " + processId + " sent message of type " + messageType + " to process " + receiverId);
            process.receiveMessage(processId, messageType, timestamp);
        } 
        catch (MalformedURLException | RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveMessage(int senderId, String messageType, int[] timestamp) {
        incrementClock();
        updateClock(timestamp);

        System.out.println("Process " + processId + " received message of type " + messageType + " from process " + senderId);
        MaekawaMessage message = new MaekawaMessage(senderId, messageType, timestamp);;

        switch (messageType) {
            case "REQUEST": {
                if (!granted) {
                    currentGrantMessage = message;
                    granted = true;
                    sendGrant(senderId);
                } 
                else receivedRequests.add(message);
                break;
            }
            case "GRANT": {
                grantSet.add(senderId);
                if (grantSet.size() == requestSet.size()) {
                    waiting = false;
                    observer.inform();
                    criticalSection();
                    sendReleases();
                }
                break;
            }
            case "RELEASE": {
                granted = false;
                if (!receivedRequests.isEmpty()) {
                    currentGrantMessage = receivedRequests.poll();
                    granted = true;
                    sendGrant(currentGrantMessage.senderId);
                }
                break;
            }
        }
    }
}
