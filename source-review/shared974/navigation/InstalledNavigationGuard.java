package net.runelite.client.plugins.microbot.questcommon.navigation;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;

/** Connects the separately reviewed installed hooks; absent/partial installs stay unavailable. */
public final class InstalledNavigationGuard implements NavigationMicrobotDriver.GuardRuntime {
    private static final String ROOT="net.runelite.client.plugins.microbot.";
    private static final String GUARD=ROOT+"questcommon.navigation.guard.RouteInputGuard";
    private static final List<String> REQUIRED=List.of(
        "util.walker.Rs2Walker","util.walker.Rs2WalkerMovement","util.walker.Rs2WalkerDoors",
        "util.walker.Rs2WalkerTransports","util.walker.Rs2PathApi","shortestpath.ShortestPathPlugin",
        "shortestpath.pathfinder.Pathfinder","Microbot","util.mouse.Mouse",
        "util.mouse.VirtualMouse","util.keyboard.Rs2Keyboard","questcommon.navigation.guard.RouteInputGuard");
    private String failure;
    private Method enable,disable,revoke,generation,approve;
    private InstalledNavigationGuard(){
        try{
            ClassLoader loader=InstalledNavigationGuard.class.getClassLoader();
            Properties hashes=new Properties();
            try(InputStream stream=loader.getResourceAsStream("META-INF/quest-navigation-guard.properties")){
                if(stream==null)throw new IllegalStateException("guard installation manifest missing");
                hashes.load(stream);
            }
            for(String name:REQUIRED){
                String resource=(ROOT+name).replace('.','/')+".class";
                if(!hashes.containsKey(resource))throw new IllegalStateException("guard manifest missing "+name);
            }
            for(String resource:hashes.stringPropertyNames()){
                if(!resource.startsWith(ROOT.replace('.','/')) || !resource.endsWith(".class"))
                    throw new IllegalStateException("invalid guard manifest resource");
                String expected=hashes.getProperty(resource);
                if(!expected.matches("[0-9a-f]{64}"))throw new IllegalStateException("invalid guard digest");
                Class<?> type=Class.forName(resource.substring(0,resource.length()-6).replace('/','.'),false,loader);
                if(type.getClassLoader()!=loader)throw new IllegalStateException("guard classloader mismatch: "+resource);
                try(InputStream stream=type.getResourceAsStream("/"+resource)){
                    if(stream==null || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(stream.readAllBytes())).equals(expected))
                        throw new IllegalStateException("guard installed bytecode mismatch: "+resource);
                }
            }
            Class<?> type=Class.forName(GUARD,false,loader);
            enable=type.getMethod("enable");disable=type.getMethod("disable");revoke=type.getMethod("revoke");
            generation=type.getMethod("generation");
            approve=type.getMethod("approveValidated",Pathfinder.class,WorldPoint.class,long.class,long.class);
        }catch(Exception | LinkageError ex){failure="Navigation guard unavailable: "+ex;}
    }
    public static InstalledNavigationGuard discover(){return new InstalledNavigationGuard();}
    @Override public String installationFailure(){return failure;}
    private Object call(Method method,Object... args){
        if(failure!=null)throw new IllegalStateException(failure);
        try{return method.invoke(null,args);}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Guard invocation failed",ex);}
    }
    @Override public void enable(){call(enable);}
    @Override public void disable(){call(disable);}
    @Override public void revoke(){call(revoke);}
    @Override public long generation(){return (Long)call(generation);}
    @Override public boolean approveValidated(Pathfinder route,WorldPoint target,long ttl,long version){
        return (Boolean)call(approve,route,target,ttl,version);
    }
}
