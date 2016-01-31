/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cesi.server.endpoint;


import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import javax.annotation.*;
import javax.ejb.*;
import javax.json.*;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author asbriglio
 * 
 */
@ServerEndpoint("/chat")
@Singleton //une seule instance par application
@Lock(LockType.READ)
public class ChatEndpoint {
    
    //retourne un Set thread-safe - liste des client connectés.
    private final Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());//peer :le côté communicant avec le endpoint donc ici le client.
    
    private final Set<String> users = Collections.synchronizedSet(new HashSet<>());//liste des utilisateurs ayant rejoint le chat
    
    @OnOpen
    public void onOpen(Session peer){
            //ajout du client navigateur à la liste des clients
            peers.add(peer);
    }
	
    @OnClose
    public void onClose(Session peer){
            //suppression du client lors de la fermeture d'une connexion avec ce pair.
            peers.remove(peer);
    }
    
    @OnMessage
    public void onMessage(String message) {
        try(JsonReader jreader = Json.createReader(new StringReader(message))){//try-with-resource

            JsonObject obj = jreader.readObject();//retourne la structure Json sous forme d'un objet JSON
            //si content n'est pas défini - si le message correspond à une jonction de chat
            if(obj.getJsonString("content")==null){
              //ajout de l'utilisateur à la liste des utilisateurs chatant
              users.add(obj.getString("user"));
              //obtention d'un builder de tableau JSON
              JsonArrayBuilder userListBuilder = Json.createArrayBuilder();
              for(String user : users){
                  userListBuilder.add(user);//ajout des utilisateurs ayant joint le chat
              }
              //création d'un objet JSON contenant la liste des utilisateurs ayant joint le chat
              JsonObject msg = Json.createObjectBuilder()
                                    .add("userList",userListBuilder.build())
                                      .build();
              StringWriter sw = new StringWriter();//flux dans lequel on peut écrire
              //assignation  du flux d'écriture à un writer JSON
              try (JsonWriter jw = Json.createWriter(sw)) {
                  //écriture de l'objet JSON dans le flux d'écriture
                  jw.writeObject(msg);
              }
              //obtention d'une chaine au format JSON contenant la liste des utilisateurs ayant joint le chat
               message = sw.toString(); 
            }//fin if

            //fermeture auto du lecteur Json
            }catch(JsonException je){ //exception contrôlée n'a pas à être gérée - indique un pb durant le traitement Json
                    System.out.println("ce n'est pas un message Json");
            }

        //on dispatche le message à tous les clients connectés
        for(Session peer : peers){
            /*getAsyncRemote retourne une référence au point de terminaison remote 
            ici le navigateur client -le pair) de type RemoteEndpoint.Async
            l'objet obtenu fournit des méthodes non blocantes d'envoi de message.
            Dés que le message est envoyé, on peut continuer le traitement.
            Autrement dit, la méthode sendXXX retourne dés que l'ensemble du message
            a été écrit sur la connexion
            */ 
            peer.getAsyncRemote().sendText(message);//envoi asynchrone du message au pair (remote endpoint)
        }
        System.out.println(message);
    }   
    
    @PostConstruct
    void init(){
        System.out.println(String.format("instance %s créée", this.getClass().getSimpleName()));
    }
    
    @PreDestroy
    void destroy(){
        users.clear();
        System.out.println(String.format("instance %s détruite", this.getClass().getSimpleName()));
    }
    
}
