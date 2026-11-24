package fr.studioniko.network;
import java.util.*;
public class AgentTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static AgentEngine.Reading r(String tech,String p,int power,long age,boolean serving,String id){return new AgentEngine.Reading(tech,"B3","208-10",p,power,-7,15,age,serving,id);}
 public static void main(String[] args){
  var l=List.of(r("LTE","secret",-95,500,true,"private-cell"));
  var h=new ArrayList<AgentEngine.Reading>();for(int i=0;i<3;i++){h.add(r("LTE","Salon",-95,0,true,"s"+i));h.add(r("LTE","Fenêtre",-85,0,true,"f"+i));}
  var x=AgentEngine.run(l,h,"mobile",true).display();check(x.contains("Indication 5G, mais aucune"),"icon must not imply NR");check(x.contains("Fenêtre : médiane -85"),"compare places");check(x.contains("Aucun débit mesuré"),"no fake speed");
  check(AgentEngine.run(List.of(r("NR","",-80,50000,true,"a")),List.of(),"mobile",true).display().contains("Aucune mesure servante fraîche"),"stale data");
  check(AgentEngine.run(List.of(r("NR","",-80,100,true,"a")),List.of(),"mobile",true).display().contains("Cellule NR servante rapportée"),"fresh serving NR");
  check(AgentEngine.run(List.of(r("NR","",-80,100,false,"a")),List.of(),"mobile",true).display().contains("Aucune mesure servante fraîche"),"neighbor not serving");
  check(AgentEngine.run(List.of(r("LTE","",Integer.MAX_VALUE,100,true,"a")),List.of(),"mobile",false).display().contains("Aucune mesure servante fraîche"),"missing signal");
  var c=AgentEngine.modelContext(l,"mobile",true);check(!c.contains("secret")&&!c.contains("private-cell")&&!c.contains("208-10"),"privacy");
  check(Math.abs(RadioMath.lteMHz(1501)-1835.1)<0.0001,"LTE B3");check(Math.abs(RadioMath.lteMHz(9235)-760.5)<0.0001,"LTE B28");
  System.out.println("9 assertions passed");
 }
}
