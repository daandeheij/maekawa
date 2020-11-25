import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MaekawaProcessRMI extends Remote {
    /**
     * This method is called when a process receives a message from a process.
     * @param senderId The ID of the process that sent the message.
     * @param content The content of the message.
     * @param timestamp The time at which the message was sent.
     * @throws RemoteException
     */
    void receiveMessage(int senderId, String content, int[] timestamp) throws RemoteException;
}
