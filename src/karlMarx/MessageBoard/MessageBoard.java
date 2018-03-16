package karlMarx.MessageBoard;

import java.util.ArrayList;
import java.util.List;

public class MessageBoard {
    List<Message> activeMessages;

    public MessageBoard() {
        activeMessages = new ArrayList<>();
    }

    public synchronized void addMessage(Message m) {
        activeMessages.add(m);
    }

    public synchronized void removeMessage(Message m) {
        activeMessages.remove(m);
    }

    public List<Message> messagesForAgent(int agent, int lastMessageId) {
        List<Message> response = new ArrayList<>();
        for (int i = activeMessages.size() - 1; i >= lastMessageId; i--) {
            if (activeMessages.get(i).canApplyFor(agent)) {
                response.add(activeMessages.get(i));
            }
        }
        return response;
    }
}
