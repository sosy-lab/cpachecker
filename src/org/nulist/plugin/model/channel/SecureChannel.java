package org.nulist.plugin.model.channel;

/**
 * @ClassName SecureChannel
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 4/26/19 10:42 AM
 * @Version 1.0
 **/
public class SecureChannel implements AbstractChannel {
    public String source;
    public String destination;
    public String messageType;

    public SecureChannel(String source, String destination, String messageType){
        this.source = source;
        this.destination = destination;
        this.messageType = messageType;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    @Override
    public String getMessageType() {
        return messageType;
    }

    @Override
    public String getChannelType() {
        return Secure;
    }

}
