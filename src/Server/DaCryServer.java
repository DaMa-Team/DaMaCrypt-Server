/*
 Copyright (C) 2013  Marcel Hollerbach, Daniel Haß

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import Client.UserHandling.ChatSession;

/**
 * DaCryServer
 * 
 * The Server Side Software from DaMaCrypt.
 * 
 * For each Client a ClientHandling Thread will be opened.
 * 
 * @author Marcel Hollerbach
 */
public class DaCryServer extends Thread implements Runnable {
	/**
	 * The Server socket
	 */
	private ServerSocket _server;
	/**
	 * ID Handling for the ClientHandlers
	 */
	private IDGenerater handlerID_generator;
	/**
	 * The list with openHandlers
	 */
	private ArrayList<ClientHandling> openhandlers;
	/**
	 * ID Handling for the Chatsessions
	 */
	private IDGenerater chatID_generator;
	/**
	 * The List with the open Chats
	 */
	private ArrayList<ChatSession> openChats;

	/**
	 * This will set up the Server Algorithm around the given ServerSocket
	 * 
	 * @param server
	 *            ServerSocket
	 */
	public DaCryServer(ServerSocket server) {
		_server = server;
		/**
		 * Init all the lists etc.
		 */
		openhandlers = new ArrayList<ClientHandling>();
		openChats = new ArrayList<ChatSession>();
		chatID_generator = new IDGenerater(1000);
		handlerID_generator = new IDGenerater(1000);
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				Socket clientsocket = _server.accept();

				System.out.println("New Client, IP:"
						+ clientsocket.getInetAddress());

				int id = handlerID_generator.getNewID();

				ClientHandling clienthandler = new ClientHandling(clientsocket,
						this, id);

				addClient(clienthandler);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This will add the given clienthandler to the Server List and will start
	 * the given clienthandler
	 * 
	 * @param clienthandler
	 */
	public void addClient(ClientHandling clienthandler) {
		openhandlers.add(clienthandler);
		clienthandler.start();
	}

	/**
	 * This will remove the given Clienthanling.
	 * 
	 * The ID of the handling will be released. This means a upcoming client can
	 * get the ID of this old Clienthandling.
	 * 
	 * Each ChatSession which is linked with this ClientHandling will be removed
	 * from the Server. The Partners of this ChatSession will be told to delete
	 * the ChatSession. The IDs of the ChatSession will be released.
	 * 
	 * The Socket of the given ClientHandling will be closed.
	 * 
	 * After this happens the Server event nameUpdated will be called.
	 * 
	 * @param clientHandling
	 */
	public void removeClient(ClientHandling clientHandling) {
		handlerID_generator.releaseID((int) clientHandling.getClientId());
		openhandlers.remove(clientHandling);
		clientHandling.interrupt();
		System.out.println(clientHandling.getSocket().getInetAddress()
				+ " quit");
		ArrayList<ChatSession> removal = new ArrayList<ChatSession>();
		for (ChatSession session : openChats) {
			if (session.getInitiator().getUser().getClientId() == clientHandling
					.getClientId()
					|| session.getPartner().getUser().getClientId() == clientHandling
							.getClientId())
				removal.add(session);

		}
		for (ChatSession chatSession : removal) {
			checkRemove(chatSession.getId());
			removeChatSession((int) chatSession.getId());
		}
		try {
			clientHandling.getSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		nameUpdated();
	}

	/**
	 * Part of the removeClient Algorithm. Will check if the ChatSession with
	 * the special id ID should be removed out of the ClientHandling
	 * 
	 * @param id
	 *            The ID of the ChatSession to check for
	 */
	private void checkRemove(long id) {
		for (ClientHandling clienthandler : openhandlers) {
			clienthandler.checkForRemoval(id);
		}
	}

	/**
	 * Will call every client to sent the new OnlineList.
	 */
	public void nameUpdated() {
		for (ClientHandling clienthandler : openhandlers) {
			try {
				clienthandler.sentOnlineList();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Getter of the OpenHandlers list.
	 * 
	 * Editing this list by hand is a worse idea.
	 * 
	 * @return
	 */
	public ArrayList<ClientHandling> getOpenhandlers() {
		return openhandlers;
	}

	/**
	 * 
	 * Will search the ChatSession with this ID.
	 * 
	 * @param id
	 *            The ID to search for
	 * @return ChatSession with the special ID
	 */
	public ChatSession getChatSession(int id) {
		for (ChatSession session : openChats) {
			if (id == session.getId()) {
				return session;
			}
		}
		return null;
	}

	/**
	 * Will remove the ChatSession and release the ID.
	 * 
	 * @param id
	 *            The ID of the ChatSession to release
	 */
	public void removeChatSession(int id) {
		ChatSession searched = getChatSession(id);
		if (searched != null) {
			chatID_generator.releaseID(id);
			openChats.remove(searched);
		}
	}

	/**
	 * This will check if every Cry field of the ChatSession is filled.
	 * 
	 * If it is the clients will be forced to sent them to the Client side.
	 * 
	 * @param chatsessionindex
	 * @throws IOException
	 */
	public void cryCheck(int chatsessionindex) throws IOException {
		ChatSession s = getChatSession(chatsessionindex);
		if (s.getPartner().getCry() != null
				&& s.getInitiator().getCry() != null) {
			getClient(s.getInitiator().getUser().getClientId())
					.writeChatSession(s);
			getClient(s.getPartner().getUser().getClientId()).writeChatSession(
					s);
		}
	}

	/**
	 * Will add this ChatSession to the list and return a unique ID.
	 * 
	 * @param s
	 * @return
	 */
	public int registerChatSession(ChatSession s) {
		openChats.add(s);
		return chatID_generator.getNewID();
	}

	/**
	 * Will return the Client with a special id.
	 * 
	 * @param id
	 *            The Special ID.
	 * @return The Client with this ID.
	 */
	public ClientHandling getClient(long id) {
		for (ClientHandling client : openhandlers) {
			if (client.getClientId() == id) {
				return client;
			}
		}
		return null;
	}
}
