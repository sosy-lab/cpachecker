package org.nulist.plugin.model.channel;

/**
 * @ClassName AbstractChannel
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 4/26/19 10:42 AM
 * @Version 1.0
 **/
public interface AbstractChannel {
    public final String Authentic= "auth";// a channel that the adversary can read the communicated message but cannot modify the message or its sender
    public final String Confidential= "conf";// a channel that the adversary cannot learn a message sent on the channel. However, he can change the sender of a meesage or send arbitray messages from his own knowledge on it
    public final String Insecure= "ins";// a channel that the adversary can do anything above
    public final String Secure= "sec";// a channel that is both authentic and confidential

    public String getSource();
    public String getDestination();
    public String getMessageType();
    public String getChannelType();
}
