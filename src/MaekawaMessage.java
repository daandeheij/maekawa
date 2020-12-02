import java.util.Arrays;

public class MaekawaMessage implements Comparable<MaekawaMessage> {
    public int senderId;
    public String messageType;
    public int inquirerId;
    public int[] timestamp;

    public MaekawaMessage(int senderId, String messageType, int inquiringId, int[] timestamp){
        this.senderId = senderId;
        this.messageType = messageType;
        this.inquirerId = inquiringId;
        this.timestamp = timestamp;
    }

    @Override
	public int compareTo(MaekawaMessage other) {
		for (int i = 0; i < timestamp.length; i++){
            if (timestamp[i] < other.timestamp[i]) return -1;
            if (timestamp[i] > other.timestamp[i]) return 1;
        }
        return other.senderId - senderId;    
    }
    
    @Override
    public String toString(){
        return senderId + ", " + messageType + ", " + Arrays.toString(timestamp);
    }
}
