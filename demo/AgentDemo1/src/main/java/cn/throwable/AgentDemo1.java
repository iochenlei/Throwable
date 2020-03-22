package cn.throwable;


public class AgentDemo1 {
    public static void premain(String agentArgs) {
        System.out.println("I'm AgentDemo1");
        throw new RuntimeException("abc");
    }

    public static void agentmain(String agentArgs) {
        System.out.println("I'm AgentDemo1(Agentmain)");
        throw new RuntimeException("abc");
    }
}
