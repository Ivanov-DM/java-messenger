package ru.geekbrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private int id;

    public int getId() { return id; }

    public String getNick() {
        return nick;
    }

    public ClientHandler(final Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/auth ")) {
                                String[] tokens = str.split(" ");
                                String nick = SQLHandler.getNickByLoginPass(tokens[1], tokens[2]);
                                if (nick != null) {
                                    if (server.isNickBusy(nick)) {
                                        out.writeUTF("The client with such data is already authorized");
                                        continue;
                                    }
                                    out.writeUTF("/authok " + nick);
                                    this.nick = nick;
                                    this.id = SQLHandler.getIdByNick(nick);
                                    server.subscribe(this);
                                    break;
                                } else {
                                    out.writeUTF("Invalid Login / password");
                                }
                            }
                        }


                        while (true) {
                            String str = in.readUTF();
                            if(str.startsWith("/w")) {
                                String[] split = str.split(" ", 3);
                                server.sendPrivateMsg(this, split[1], split[2]);
                            } else if(str.equals("/history")) {
                                sendMsg(SQLHandler.getHistory(this.id));
                            } else {
                                server.broadcastMsg(this, str);
                            }
                            System.out.println(str);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        server.unsubscribe(this);
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
