package cn.throwable;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

public class Injector
{
    public static void main(String[] args ) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        String jvmId = "19892";
        VirtualMachine vm = VirtualMachine.attach(jvmId);
        File agentFile = new File("../AgentDemo1/target/AgentDemo1-1.0-SNAPSHOT.jar");
        vm.loadAgent(agentFile.getAbsolutePath());
        vm.detach();
    }
}
