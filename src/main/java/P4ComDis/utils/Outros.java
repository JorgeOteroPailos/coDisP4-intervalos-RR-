package P4ComDis.utils;

public abstract class Outros {
    private static final boolean estouDebuggeando=true;

    public static void debugPrint(String mensaxe){
        if(estouDebuggeando) System.out.println(mensaxe);
    }
}
