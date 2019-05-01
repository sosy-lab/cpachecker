package org.nulist.plugin.model.channel;

/**
 * @ClassName AuthenticChannel
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 4/30/19 12:35 PM
 * @Version 1.0
 **/
public class AuthenticChannel implements AbstractChannel {
    public String source;
    public String destination;
    public String messageType;

    public AuthenticChannel(String source, String destination, String messageType){
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
