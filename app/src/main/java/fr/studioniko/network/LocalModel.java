package fr.studioniko.network;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Optional real LLM inference through a user-controlled localhost OpenAI-compatible server. */
final class LocalModel {
    static String analyze(String context,String model) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8080/v1/chat/completions").openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(3000);c.setReadTimeout(90000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
        try {
            JSONArray messages=new JSONArray();messages.put(new JSONObject().put("role","system").put("content","Tu es un ingénieur radio. Réponds en français en 150 mots maximum. Donne des hypothèses distinctes des faits, des tests reproductibles et les limites. Ne prétends pas connaître le débit, l'antenne ou la 5G active sans preuve. Ne propose ni modification cachée du modem ni interception radio. Tu n'as accès à aucun outil ni réglage."));messages.put(new JSONObject().put("role","user").put("content",context));
            byte[] data=new JSONObject().put("model",model).put("messages",messages).put("max_tokens",300).put("temperature",0.2).put("stream",false).toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream o=c.getOutputStream()){o.write(data);}if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>262144)throw new IOException("Réponse trop longue");out.write(buf,0,n);}String result=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();if(result.isEmpty())throw new IOException("Réponse vide");return result;}
        } finally{c.disconnect();}
    }
}
