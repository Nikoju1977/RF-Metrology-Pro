package fr.studioniko.network;

import java.util.*;

/** Local, deterministic specialist agents. No model or network needed for safe decisions. */
final class AgentEngine {
    record Reading(String tech, String band, String operator, String place, int rsrp, int rsrq, int snr, long ageMs, boolean serving, String identity) {}
    record Finding(String agent, String evidence, String action) {}
    record Report(List<Finding> findings, String summary) {
        String display() { StringBuilder b=new StringBuilder(); for(Finding f:findings)b.append(f.agent()).append("\n").append(f.evidence()).append("\n→ ").append(f.action()).append("\n\n");return b.append("SUPERVISEUR\n").append(summary).toString(); }
    }
    private static boolean valid(int v,int min,int max){return v>=min&&v<=max;}
    static Report run(List<Reading> live,List<Reading> history,String transport,boolean icon5g){
        List<Finding> out=new ArrayList<>();
        List<Reading> fresh=new ArrayList<>(); for(Reading r:live)if(r.ageMs()>=0&&r.ageMs()<=20000&&r.serving())fresh.add(r);
        Reading r=null; for(Reading x:fresh)if(valid(x.rsrp(),-160,-30)){r=x;break;}
        if(r==null)out.add(new Finding("01 · QUALITÉ DES DONNÉES","Aucune mesure servante fraîche et exploitable.","Vérifier les autorisations et relancer une acquisition."));
        else {
            out.add(new Finding("01 · QUALITÉ DES DONNÉES","Mesure servante fraîche : "+r.tech()+" "+r.band()+" ; RSRP "+r.rsrp()+" dBm.","Ne pas assimiler les valeurs absentes à zéro."));
            String action=r.rsrp()<-105?"Comparer une fenêtre et un emplacement extérieur autorisé.":"Comparer plusieurs emplacements avant de modifier le mode réseau.";
            if(valid(r.snr(),-30,50)&&r.snr()<5)action="Rapport signal/bruit faible : comparer un autre emplacement ou horaire.";
            out.add(new Finding("02 · RADIO RF","RSRP "+r.rsrp()+" dBm ; RSRQ "+(valid(r.rsrq(),-45,20)?r.rsrq()+" dB":"indisponible")+" ; SNR/SINR "+(valid(r.snr(),-30,50)?r.snr()+" dB":"indisponible")+".",action));
        }
        boolean nr=false;int lte=0;for(Reading x:fresh){if("NR".equals(x.tech()))nr=true;else if("LTE".equals(x.tech()))lte++;}
        out.add(new Finding("03 · EXPERT 5G",nr?"Cellule NR servante rapportée (activité des données non prouvée).":icon5g?"Indication 5G, mais aucune cellule NR servante fraîche rapportée.":"Aucune cellule NR servante fraîche rapportée.",nr?"Comparer débit et stabilité sans déduire une agrégation exacte.":"Vérifier pendant un transfert mobile ; le modem peut masquer des informations."));
        out.add(new Finding("04 · PERFORMANCE", "Transport : "+transport+" ; "+lte+" cellule(s) LTE servante(s) exposée(s). Aucun débit mesuré par ce moteur.","mobile".equals(transport)?"Mesurer débit montant, latence et gigue à plusieurs horaires.":"Désactiver volontairement le Wi-Fi pour un test mobile isolé."));
        Map<String,Map<String,List<Integer>>> groups=new TreeMap<>();Set<String> seen=new HashSet<>();
        for(Reading h:history){if(!valid(h.rsrp(),-160,-30)||h.place()==null||h.place().trim().isEmpty()||h.identity()==null||h.identity().trim().isEmpty()||!seen.add(h.identity()))continue;
            groups.computeIfAbsent(h.operator()+" / "+h.tech()+" "+h.band(),k->new TreeMap<>()).computeIfAbsent(h.place(),k->new ArrayList<>()).add(h.rsrp());}
        StringBuilder compare=new StringBuilder();for(var group:groups.entrySet()){String best=null;double max=-999,second=-999;int n=0,eligible=0;for(var e:group.getValue().entrySet()){if(e.getValue().size()<3)continue;eligible++;double m=RadioMath.median(e.getValue());if(m>max){second=max;max=m;best=e.getKey();n=e.getValue().size();}else if(m>second)second=m;}if(eligible>=2){if(compare.length()>0)compare.append('\n');compare.append(group.getKey()).append(": ");if(max-second<3)compare.append("écart inférieur à 3 dB ; pas de gagnant robuste");else compare.append(best).append(" : médiane ").append(RadioMath.fmt(max)).append(" dBm (n=").append(n).append(") ; avantage indicatif ").append(RadioMath.fmt(max-second)).append(" dB");}}
        out.add(new Finding("05 · OPTIMISATION SPATIALE",compare.length()==0?"Comparaison insuffisante : au moins deux lieux et trois mesures uniques par lieu, même opérateur/tech/bande.":compare.toString(),"Confirmer avec les mêmes conditions et plusieurs horaires ; le meilleur RSRP n'est pas forcément le meilleur débit."));
        out.add(new Finding("06 · CONTRÔLE & SÉCURITÉ","Aucun accès privilégié au modem ; PCI seul non unique.","Ne pas forcer de bande, ne pas déduire une antenne ou un signal ISAC sans preuve."));
        return new Report(List.copyOf(out),r==null?"Acquisition prioritaire. Aucune conclusion de performance.":"Diagnostic automatique terminé. Recommandations indicatives ; aucune modification du modem.");
    }
    // Model receives only live aggregated radio metrics, never identifiers or place labels.
    static String modelContext(List<Reading> live,String transport,boolean icon5g){StringBuilder b=new StringBuilder("Transport: ").append(transport).append("; indication 5G: ").append(icon5g);for(Reading r:live)if(r.serving()&&r.ageMs()>=0&&r.ageMs()<=20000){b.append("\n").append(r.tech()).append(' ').append(r.band()).append(" RSRP=").append(valid(r.rsrp(),-160,-30)?r.rsrp():"inconnu").append(" RSRQ=").append(valid(r.rsrq(),-45,20)?r.rsrq():"inconnu").append(" SNR/SINR=").append(valid(r.snr(),-30,50)?r.snr():"inconnu");}return b.toString();}
}
