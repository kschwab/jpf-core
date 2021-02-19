import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.ListenerAdapter;

// enve javac -cp ".:../build/jpf.jar" MyListener.java

public class MyListener extends ListenerAdapter
{
    String lastLoc = "";

    public void classLoaded(VM vm, ClassInfo loadedClass)
    {
        System.out.println("Yes, I'm listening");
    }
}
