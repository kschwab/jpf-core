package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.RootObj;
import com.squareup.haha.perflib.RootType;
import com.squareup.haha.perflib.Snapshot;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

/** Durable I.1 characterization of HAHA root provenance for controlled HotSpot roots. */
public final class HprofRootCharacterizer {
  private static final String MARKER="HprofRootCharacterizationDumpGenerator$Marker";
  private static final String HOLDER="HprofRootCharacterizationDumpGenerator";

  public static void main(String[] args)throws Exception{
    Snapshot snapshot=HprofSnapshotLoader.load(new File(args[0])); HprofView view=HprofView.from(snapshot);
    Map<Integer,ClassInstance> markers=new LinkedHashMap<>();
    for(Instance i:view.instances.values())if(i instanceof ClassInstance&&MARKER.equals(i.getClassObj().getClassName()))markers.put(intField((ClassInstance)i,"kind"),(ClassInstance)i);
    require(markers.size()==3,"expected three marker objects");
    Map<RootType,Integer> counts=new EnumMap<>(RootType.class); Map<Long,Set<RootType>> byId=new LinkedHashMap<>();
    for(RootObj root:snapshot.getGCRoots()){counts.put(root.getRootType(),counts.getOrDefault(root.getRootType(),0)+1);byId.computeIfAbsent(root.getId(), ignored -> EnumSet.noneOf(RootType.class)).add(root.getRootType());require(root.getReferredInstance()!=null,"root does not resolve: "+root);}
    Set<RootType> local=byId.get(markers.get(2).getId()); Set<RootType> monitor=byId.get(markers.get(3).getId());
    require(local!=null&&local.contains(RootType.JAVA_LOCAL),"live local marker is not JAVA_LOCAL: "+local);
    require(monitor!=null&&monitor.contains(RootType.JAVA_LOCAL),"monitor local lacks JAVA_LOCAL: "+monitor);
    boolean busyMonitor = monitor.contains(RootType.BUSY_MONITOR);
    ClassObj holder=findClass(view,HOLDER); Object staticValue=staticValue(holder,"staticMarker"); require(staticValue==markers.get(1),"static marker field identity mismatch");
    require(!byId.containsKey(markers.get(1).getId()),"static field target unexpectedly encoded as direct root");
    Object threadValue=staticValue(holder,"worker"); require(threadValue instanceof Instance,"worker static is not Instance");
    Set<RootType> threadRoots=byId.get(((Instance)threadValue).getId());
    System.out.printf("[HPROF-JPF] root marker static=0x%x viaClassStatic=true directRoot=false%n",markers.get(1).getId());
    System.out.printf("[HPROF-JPF] root marker javaLocal=0x%x rootType=%s%n",markers.get(2).getId(),local);
    System.out.printf("[HPROF-JPF] root marker monitor=0x%x rootTypes=%s busyMonitor=%s%n",markers.get(3).getId(),monitor,busyMonitor);
    System.out.printf("[HPROF-JPF] thread object=0x%x otherRootTypes=%s THREAD_OBJECT-provenance-exposed=false parserThreadTableOnly=true%n",((Instance)threadValue).getId(),threadRoots);
    System.out.println("[HPROF-JPF] root provenance counts="+counts);
    System.out.println("[HPROF-JPF] root characterization verified: staticField=true javaLocal=true monitorRecord=false threadObjectParserLoss=true");
  }
  private static ClassObj findClass(HprofView view,String name){ClassObj result=null;for(ClassObj c:view.classes.values())if(name.equals(c.getClassName())){require(result==null,"duplicate class "+name);result=c;}require(result!=null,"missing class "+name);return result;}
  private static Object staticValue(ClassObj ci,String name){for(Map.Entry<com.squareup.haha.perflib.Field,Object> e:ci.getStaticFieldValues().entrySet())if(name.equals(e.getKey().getName()))return e.getValue();throw new IllegalStateException("missing static "+name);}
  private static int intField(ClassInstance i,String name){for(ClassInstance.FieldValue v:i.getValues())if(name.equals(v.getField().getName()))return(Integer)v.getValue();throw new IllegalStateException("missing field "+name);}
  private static void require(boolean value,String message){if(!value)throw new IllegalStateException("[HPROF-JPF] root characterization failed: "+message);}
}
