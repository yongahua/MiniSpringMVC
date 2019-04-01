package gupao.vip.Brave.mvcframework.conversion;

import org.jcp.xml.dsig.internal.SignerOutputStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TypeConversion {

    public static final Class<?> TYPE_STRING = String.class;

    public static final Class<?> TYPE_INTEGER = Integer.class;

    public static final Class<?> TYPE_DOUBLE = Double.class;

    public static final Class<?> TYPE_BYTE = Byte.class;

    public static final Class<?> TYPE_SHORT = Short.class;

    public static final Class<?> TYPE_LONG = Long.class;

    public static final Class<?> TYPE_FLOAT = Float.class;

    public static final Class<?> TYPE_CHARACTER = Character.class;

    public static final Class<?> TYPE_BOOLEAN = Boolean.class;

    private static Map<Class<?>,Object> types = new HashMap<Class<?>,Object>();

    public TypeConversion(String value) {
        try{
            types.put(TYPE_STRING,value);
            types.put(TYPE_BYTE,value.getBytes());
            types.put(TYPE_CHARACTER,value.toCharArray());
            types.put(TYPE_INTEGER,Integer.valueOf(value));
            types.put(TYPE_DOUBLE,Double.valueOf(value));
            types.put(TYPE_SHORT,Short.valueOf(value));
            types.put(TYPE_LONG,Long.valueOf(value));
            types.put(TYPE_FLOAT,Float.valueOf(value));
            types.put(TYPE_BOOLEAN,Boolean.valueOf(value));
        }catch (Exception e){
            types.put(TYPE_STRING,value);
            types.put(TYPE_BYTE,value.getBytes());
            types.put(TYPE_CHARACTER,value.toCharArray());
        }

    }

    public static Object convert(Class<?> clazz){
        if(!types.containsKey(clazz)){
            return null;
        }
        if(TYPE_STRING.equals(clazz)){
            String value = (types.get(clazz).toString().replaceAll("\\[\\]","")
                    .replaceAll("\\s",",")).replaceAll(",+",",");
            return value;
        }
        return types.get(clazz);
    }
}
