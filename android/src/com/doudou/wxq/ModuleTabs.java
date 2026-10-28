package com.doudou.wxq;
import java.util.*;
/** A module is created only once per Activity; switching keeps its current page state. */
final class ModuleTabs<T> {
 interface Factory<T>{T create(String id);}
 private final Map<String,T> pages=new LinkedHashMap<>();private String selected=SiteModules.HOME;
 void home(T value){pages.put(SiteModules.HOME,value);}
 T select(String id,Factory<T> factory){if(!SiteModules.valid(id))throw new IllegalArgumentException("Unknown module");T value=pages.get(id);if(value==null){value=factory.create(id);if(value==null)throw new IllegalStateException("Missing module");pages.put(id,value);}selected=id;return value;}
 String selected(){return selected;}
 Collection<T> values(){return new ArrayList<>(pages.values());}
 void clear(){pages.clear();selected=SiteModules.HOME;}
}
