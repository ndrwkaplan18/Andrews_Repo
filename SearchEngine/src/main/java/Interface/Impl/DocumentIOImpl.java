package Interface.Impl;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import Interface.Document;
import Interface.DocumentIO;
import java.io.*;
import java.net.URI;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.HashMap;
import com.google.gson.reflect.TypeToken;
import Interface.DocumentStore;
import org.apache.commons.codec.binary.Base64;

public class DocumentIOImpl extends DocumentIO {

    protected File baseDir;
    protected boolean deleteOnExit;
    public DocumentIOImpl(File baseDir, boolean deleteOnExit)
    {
        this.deleteOnExit = deleteOnExit;
        this.baseDir = baseDir;
    }

    public DocumentIOImpl(File baseDir){
        this.deleteOnExit = false;
        this.baseDir = baseDir;
    }

    static class DocumentSerializer implements JsonSerializer<Document>{

        @Override
        public JsonElement serialize(Document document, Type type, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            Gson gson = new Gson();
            object.add("hashCode",new JsonPrimitive(document.getDocumentHashCode()));
            object.add("compressedDoc",new JsonPrimitive(Base64.encodeBase64String(document.getDocument())));
            object.add("uri", new JsonPrimitive(document.getKey().toString()));
            object.add("format", gson.toJsonTree(document.getCompressionFormat(),document.getCompressionFormat().getClass()));
            object.add("wordMap", gson.toJsonTree(document.getWordMap(),document.getWordMap().getClass()));
            return object;
        }

    }

    static class DocumentDeserializer implements JsonDeserializer<Document>{

        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            try {
                JsonObject object = jsonElement.getAsJsonObject();
                Type hashMapType = new TypeToken<HashMap<String, Integer>>(){}.getType();
                Type compressionFormatType = new TypeToken<DocumentStore.CompressionFormat>(){}.getType();
                int hashCode = object.get("hashCode").getAsInt();
                byte[] compressedDoc = Base64.decodeBase64(object.get("compressedDoc").getAsString());
                URI uri = new URI(object.get("uri").getAsString());
                DocumentStore.CompressionFormat format = new  Gson().fromJson(object.get("format"), compressionFormatType);
                HashMap<String, Integer> wordMap = new Gson().fromJson(object.get("wordMap"), hashMapType);

                return new DocumentImpl(compressedDoc, uri, hashCode, format, wordMap);

            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    /**
     * @param doc to serialize
     * @return File object representing file on disk to which document was serialized
     */
    public File serialize(Document doc)
    {
        Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, new DocumentSerializer()).setPrettyPrinting().create();
        Type docType = new TypeToken<Document>(){}.getType();
        JsonElement json = gson.toJsonTree(doc,docType);
        File destination;
        if(getFile(doc.getKey()).exists()){
            destination = overwriteFile(doc.getKey());
        }
        else{
            destination = getFile(doc.getKey());
            destination.getParentFile().mkdirs();
        }
        if(deleteOnExit) destination.deleteOnExit();
        try {
            writeJsonFile(destination, json, gson);
            return destination;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     10
     Requirements Version: May 12, 2019
     * @param uri of doc to deserialize
     * @return deserialized document object
     */
    public Document deserialize(URI uri)
    {

        try {

            File destination = getSerializedFile(uri);
            FileReader reader = new FileReader(destination);
            JsonReader jsonReader = new JsonReader(reader);
            JsonElement element = new JsonParser().parse(jsonReader);
            Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, new DocumentDeserializer()).create();
            Type docType = new TypeToken<Document>(){}.getType();
            jsonReader.close();
            destination.delete();
            return gson.fromJson(element, docType);
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    protected File getFile(URI uri){
        File file;
        if(uri.toString().indexOf("//") != -1){
            file = new File(baseDir, uri.toString().substring(uri.toString().indexOf("//")+1) + ".json");
        }
        else{
            file = new File(baseDir, uri.toString() + ".json");
        }
        return file;
    }

    protected void writeJsonFile(File destination, JsonElement json, Gson gson) throws Exception{
        FileOutputStream fos = new FileOutputStream(destination);
        JsonWriter jsonWriter = gson.newJsonWriter(new OutputStreamWriter(fos,"UTF-8"));
        gson.toJson(json, jsonWriter);
        jsonWriter.close();
        fos.close();
    }

    protected File overwriteFile(URI uri){
        File file;
        int j = 0;
        do {
            j++;
            if (uri.toString().indexOf("//") != -1) {
                file = new File(baseDir, uri.toString().substring(uri.toString().indexOf("//") + 1) + "(" + j + ").json");
            } else {
                file = new File(baseDir, uri.toString() + "(" + j + ").json");
            }
        } while (file.exists());

        return file;
    }

    protected File getSerializedFile(URI uri){
        File file = getFile(uri);
        File prevFile;
        int j = 0;
        while(true) {
            j++;
            prevFile = file;
            if (uri.toString().indexOf("//") != -1) {
                file = new File(baseDir, uri.toString().substring(uri.toString().indexOf("//") + 1) + "(" + j + ").json");
            } else {
                file = new File(baseDir, uri.toString() + "(" + j + ").json");
            }
            if(!file.exists()){
                return prevFile;
            }
        }
    }
}
