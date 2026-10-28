package com.doudou.wxq;

import org.json.*;

/** Reads only data literals, never evaluates downloaded JavaScript. */
final class JsDataReader {
    private final String s;private int p;private int depth;
    JsDataReader(String s,int start){this.s=s;p=start;}
    int position(){return p;}
    private void space(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
    private char peek(){space();return p<s.length()?s.charAt(p):'\0';}
    private void need(char c){if(peek()!=c)throw new IllegalArgumentException("资料格式变化 @"+p);p++;}
    Object read() throws Exception{
        if(++depth>128)throw new IllegalArgumentException("资料嵌套过深");
        try{
            char c=peek();
            if(c=='['){p++;JSONArray a=new JSONArray();if(peek()==']'){p++;return a;}while(true){a.put(read());if(peek()==']'){p++;return a;}need(',');}}
            if(c=='{'){p++;JSONObject o=new JSONObject();if(peek()=='}'){p++;return o;}while(true){String k=peek()=='"'||peek()=='\''?string():word();need(':');o.put(k,read());if(peek()=='}'){p++;return o;}need(',');}}
            if(c=='"'||c=='\'')return string();
            if(c=='!'){p++;char n=peek();if(n!='0'&&n!='1')throw new IllegalArgumentException("未知布尔值");p++;return n=='0';}
            if(c=='-'||Character.isDigit(c)){int start=p++;while(p<s.length()&&"0123456789.eE+-".indexOf(s.charAt(p))>=0)p++;String n=s.substring(start,p);if(n.indexOf('.')>=0||n.indexOf('e')>=0||n.indexOf('E')>=0)return Double.valueOf(n);return Long.valueOf(n);}
            String w=word();if(w.equals("null"))return JSONObject.NULL;if(w.equals("true"))return true;if(w.equals("false"))return false;
            throw new IllegalArgumentException("不支持的资料表达式");
        }finally{depth--;}
    }
    private String word(){space();int start=p;while(p<s.length()&&(Character.isLetterOrDigit(s.charAt(p))||s.charAt(p)=='_'||s.charAt(p)=='$'))p++;if(start==p)throw new IllegalArgumentException("资料键不完整");return s.substring(start,p);}
    private String string() throws Exception{space();int start=p;char quote=s.charAt(p++);boolean escaped=false;while(p<s.length()){char c=s.charAt(p++);if(escaped){escaped=false;continue;}if(c=='\\'){escaped=true;continue;}if(c==quote)return String.valueOf(new JSONTokener(s.substring(start,p)).nextValue());}throw new IllegalArgumentException("资料字符串不完整");}
}
