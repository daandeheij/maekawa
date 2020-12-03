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
    public int period;
    public int offset;
    public int duration;
    public int numberOfGrants;
    public boolean granted;
    public boolean postponed;
    public boolean inquiring;
    public MaekawaMessage currentGrant;

    protected MaekawaProcess(int processId, int numberOfProcesses, Set<Integer> requestSet, int offset, int period, int duration) throws RemoteException {
        this.processId = processId;
        this.clock = new int[numberOfProcesses];
        this.receivedRequests = new PriorityQueue<MaekawaMessage>();
        this.requestSet = requestSet;
        this.offset = offset;
        this.period = period;
        this.duration = duration;
    }

    @Override
    public void run() {
        wait(offset);
        while (true){
            multicastRequest();
            wait(period);
        }
    }

    /**
     * Lets the process wait for the given duration.
     * @param duration The amount of time in milliseconds that the process should wait.
     */
    public void wait(int duration){
        try {
            Thread.sleep(duration);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the critical section of this process.
     */
    public void criticalSection() {
        System.out.println("Process " + processId + " entering critical section with request set: " + requestSet.toString());
        wait(duration);
        System.out.println("Process " + processId + " finished execution of critical section with request set: " + requestSet.toString());
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

    /**
     * Sends request messages to all processes in the request set.
     */
    public void multicastRequest() {
        int[] timestamp = incrementClock();
        for (int resourceId : requestSet) {
            sendMessage(resourceId, "REQUEST", timestamp);
        }
    }

    /**
     * Sends release messages to all processes in the request set.
     */
    public void multicastRelease() {
        numberOfGrants = 0;
        int[] timestamp = incrementClock();
        for (int resourceId : requestSet) {
            sendMessage(resourceId, "RELEASE", timestamp);
        }
    }

    /**
     * Sends a grant to the process with the given process ID.
     * @param receiverId The ID of the process to receive the grant.
     */
    public void sendGrant(int receiverId) {
        int[] timestamp = incrementClock();
        sendMessage(receiverId, "GRANT", timestamp);
    }

    /**
     * Sends a postpone to the process with the given process ID.
     * @param receiverId The ID of the process to receive the postpone.
     */
    public void sendPostpone(int receiverId) {
        int[] timestamp = incrementClock();
        sendMessage(receiverId, "POSTPONE", timestamp);
    }

    /**
     * Sends an inquire to the process with the given process ID.
     * @param receiverId The ID of the process to receive the inquire.
     */
    public void sendInquire(int receiverId) {
        int[] timestamp = incrementClock();
        sendMessage(receiverId, "INQUIRE", timestamp);
    }

    /**
     * Sends a relinquish to the process with the given process ID.
     * @param receiverId The ID of the process to receive the inquire.
     */
    public void sendRelinquish(int receiverId) {
        int[] timestamp = incrementClock();
        sendMessage(receiverId, "RELINQUISH", timestamp);
    }
    
    /**
     * Sends a message to the process with the given process ID.
     * @param receiverId The ID of the process to send the message to.
     * @param messageType The type of the message.
     * @param timestamp The timestamp of the message.
     */
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
        MaekawaMessage message = new MaekawaMessage(senderId, messageType, timestamp);

        switch (messageType) {
            case "REQUEST": {
                int j = message.senderId;
                if (!granted) {
                    currentGrant = message;
                    granted = true;
                    sendGrant(j);
                } 
                else {
                    receivedRequests.add(message);
                    MaekawaMessage earliestRequest = receivedRequests.peek();
                    if ((currentGrant.compareTo(message) < 0) || (earliestRequest.compareTo(message) < 0)){
                        sendPostpone(j);
                    }
                    else {
                        if (!inquiring){
                            inquiring = true;
                            int l = currentGrant.senderId;
                            sendInquire(l);
                        }
                    }
                }
                break;
            }
            case "GRANT": {
                numberOfGrants++;
                if (numberOfGrants == requestSet.size()) {
                    postponed = false;
                    criticalSection();
                    multicastRelease();
                }
                break;
            }
            case "INQUIRE": {
                int j = message.senderId;
                while (!(postponed) && !(numberOfGrants == requestSet.size())){
                    //wait(10);
                }
                if (postponed) {
                    numberOfGrants--;
                    sendRelinquish(j);
                }
                break;
            }
            case "RELINQUISH": {
                inquiring = false;
                granted = false;
                receivedRequests.add(currentGrant);
                currentGrant = receivedRequests.poll();
                int j = currentGrant.senderId;
                granted = true;
                sendGrant(j);
                break;
            }
            case "RELEASE": {
                granted = false;
                inquiring = false;
                if (!receivedRequests.isEmpty()) {
                    currentGrant = receivedRequests.poll();
                    int j = currentGrant.senderId;
                    granted = true;
                    sendGrant(j);
                }
                break;
            }
            case "POSTPONE": {
                postponed = true;
                break;
            }
        }
    }
}
