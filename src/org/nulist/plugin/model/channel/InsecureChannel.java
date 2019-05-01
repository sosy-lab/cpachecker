package org.nulist.plugin.model.channel;

/**
 * @ClassName InsecureChannel
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 4/26/19 10:42 AM
 * @Version 1.0
 **/
public class InsecureChannel implements AbstractChannel {
    public String source;
    public String destination;
    public String messageType;

    public InsecureChannel(String source, String destination, String messageType){
        this.source = source;
        this.destination = destination;
        this.messageType = messageType;
    }

    @Override
    public String getChannelType() {
        return Insecure;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getMessageType() {
        return messageType;
    }

    @Override
    public String getDestination() {
        return destination;
    }
}
