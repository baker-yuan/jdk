package cn.baker.threadlocal;



/**
 * 苹果
 */
public class Apple {
     private String name;

     /**
      * 对象回收最后调用方法，回收时执行
      */
     @Override
     protected void finalize() throws Throwable {
         super.finalize();
         System.out.println("Apple "+ name + " finalize");
     }

     public Apple(String name){ this.name = name; }
     public String getName() { return name; }
     public void setName(String name) { this.name = name; }
     @Override
     public String toString() { return "Apple{" + "name='" + name + '\'' + '}'+", hashcode:"+this.hashCode(); }
}


