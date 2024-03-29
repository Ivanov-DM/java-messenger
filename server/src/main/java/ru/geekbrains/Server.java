package ru.geekbrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {

    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients;

    public Server() {
        try{
            SQLHandler.connect();
            serverSocket = new ServerSocket(8081);
            clients = new Vector<ClientHandler>();
            System.out.println("Server is started");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client is connected");
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            SQLHandler.disconnect();
        }
    }

    // Рассылка широковещательного сообщения
    public void broadcastMsg(ClientHandler client, String msg) {
        String outMsg = client.getNick() + ": " + msg;
        SQLHandler.addHistory(client.getId(), -1, outMsg);
        for (ClientHandler o: clients) {
            o.sendMsg(outMsg);
        }
    }

    //Отправка приватного сообщения
    public void sendPrivateMsg(ClientHandler from, String to, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(to)) {
                o.sendMsg("Private msg from " + from.getNick() + ": " + msg);
                from.sendMsg("Private msg to " + to + ": " + msg);
                SQLHandler.addHistory(from.getId(), o.getId(), "Private msg from " + from.getNick() + ": " + msg);
                SQLHandler.addHistory(from.getId(), o.getId(), "Private msg to " + o.getNick() + ": " + msg);
                return;
            }
        }
        from.sendMsg("User " + to + " is offline");
    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientslist ");
        for(ClientHandler o : clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.substring(0, sb.length() - 1);
        for(ClientHandler o : clients) {
            o.sendMsg(out);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientsList();
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (nick.equals(o.getNick())) {
                return true;
            }
        }
        return false;
    }
}


