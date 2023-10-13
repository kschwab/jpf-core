import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import com.sun.management.HotSpotDiagnosticMXBean;

class HeapDump
{
    private static final String HotSpotBeanName = "com.sun.management:type=HotSpotDiagnostic";
    private static volatile HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean;

    public static class FindMe {
        String myName;

        public FindMe(String myName) {
            this.myName = myName;
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("heapdump.hprof");
        FindMe me = new FindMe("find this val");
        System.out.println("Pre heap-dump value: " + me.myName);
        if (file.exists()) {
            file.delete();
        }
        System.out.println("Pre heap-dump value: " + me.myName);
        // @todo will need to map this to something that is ignored in JPF
        // generateHeapDump("heapdump.hprof", true);
        // System.out.println("Post heap-dump value: " + file);
    }

    private static void generateHeapDump(String fileName, boolean isLive) throws IOException {
        if (hotSpotDiagnosticMXBean == null) {
            synchronized (HeapDump.class) {
                hotSpotDiagnosticMXBean = getHotSpotDiagnosticMXBean();
            }
        }

        hotSpotDiagnosticMXBean.dumpHeap(fileName,isLive);
    }

    private static HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws IOException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServer, HotSpotBeanName, HotSpotDiagnosticMXBean.class);
    }
}
