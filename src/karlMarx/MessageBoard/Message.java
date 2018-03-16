package karlMarx.MessageBoard;

public abstract class Message {
    protected static int idCount = 0;
    protected int id;
    protected int[] receivers; // possible optimization, save all in one int
    MessageStatus status; // package private

    public Message() {
        id = idCount++;
    }

    public boolean canApplyFor(int agent) {
        for (int i = 0; i < receivers.length; i++) {
            if (receivers[i] == agent) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean bidOnJob() {
        if (status == MessageStatus.PENDING) {
            status = MessageStatus.IN_PROGRESS;
            return true;
        }
        return false;
    }

    public synchronized void failJob() {
        status = MessageStatus.FAILED;
        // TODO: possibly contact the agent in charge, and say the request failed
    }

    public synchronized void succeedJob() {
        status = MessageStatus.DONE;
        // TODO: possibly contact the agent in charge, and say the request succeed
    }
}
