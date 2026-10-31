package app.veyra;
import android.content.Context;import java.util.*;
final class HomeLayout {
 static final String[] SECTIONS={"Continue watching","My list","For your taste","Highly rated","Newer stories","Worth revisiting","Something unexpected","Explore the collection","Discover something different","Keep exploring"};
 static List<String> order(Context c){LinkedHashSet<String> ordered=new LinkedHashSet<>();List<String> allowed=Arrays.asList(SECTIONS);for(String name:ProfileStore.prefs(c,"home-layout").getString("order","").split("\n"))if(allowed.contains(name))ordered.add(name);ordered.addAll(allowed);return new ArrayList<>(ordered);}
 static boolean visible(Context c,String name){return !ProfileStore.prefs(c,"home-layout").getBoolean("hide:"+name,false);}
 static void moveUp(Context c,String name){List<String> order=order(c);int index=order.indexOf(name);if(index>0){Collections.swap(order,index,index-1);ProfileStore.prefs(c,"home-layout").edit().putString("order",android.text.TextUtils.join("\n",order)).apply();}}
}
