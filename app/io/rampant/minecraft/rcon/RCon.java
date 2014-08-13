package io.rampant.minecraft.rcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This class implements the communication with MineCraft using the RCon
 * protocol.
 *
 * @author vincent
 */
public class RCon {

	private static final int COMMAND_TYPE = 2;
	/**
	 * The value of a login packet type.
	 */
	private static final int LOGIN_TYPE = 3;

	/**
	 * The request id that will be used for this communication channel.
	 */
	private final int requestId;

	/**
	 * The socket used for the communication.
	 */
	private final Socket socket;

	/**
	 * The input stream for reading the data from MineCraft.
	 */
	private final InputStream inputStream;

	/**
	 * The output stream for sending the data to MineCraft.
	 */
	private final OutputStream outputStream;

	/**
	 * The object to be used for synchronization of the communication using the
	 * socket. This means also the inputStream and outputStream.
	 */
	private final Object syncObject = new Object();

	/**
	 * Create a new communication channel to MineCraft using the RCon protocol.
	 * The channel will try to connect to the defined host and port and initiate
	 * the login sequence for authentication with MineCraft.
	 *
	 * @param host
	 * 		The name or IP address of the host.
	 * @param port
	 * 		The port.
	 * @param password
	 * 		The password.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public RCon(final String host, final int port, final String password) throws IOException, AuthenticationException {
		super();
		final Random random = new Random();
		requestId = random.nextInt();
		socket = new Socket(host, port);
		outputStream = socket.getOutputStream();
		inputStream = socket.getInputStream();
		final byte[] response = send(LOGIN_TYPE, password.getBytes());
		assert response.length == 0;
	}

	/**
	 * Blacklists the name player from the server so that they can no longer
	 * connect.
	 * <p>
	 * Note: Bans supersede any white-listing in place.
	 *
	 * @param player
	 * 		The player to be banned.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void ban(final String player) throws IOException, AuthenticationException {
		final String response = send("ban " + player);
		assert "".equals(response);
	}

	/**
	 * Blacklists a host so that all subsequent connections from it are
	 * rejected.
	 *
	 * @param host
	 * 		The host to be banned.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void banIp(final String host) throws AuthenticationException, IOException {
		final String response = send("ban-ip " + host);
		assert "".equals(response);
	}

	/**
	 * Shows the names of all the banned hosts.
	 *
	 * @return A list with the banned hosts.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public String[] banIPList() throws IOException, AuthenticationException {
		final String response = send("banlist ips");
		final int colonPosition = response.indexOf(':');
		final String ipResponse = response.substring(colonPosition + 1).trim();
		return "".equals(ipResponse) ? new String[0] : ipResponse.split(",\\s+");
	}

	/**
	 * Shows the names of all the banned players.
	 *
	 * @return A list with the banned players.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public String[] banList() throws IOException, AuthenticationException {
		final String response = send("banlist");
		final int colonPosition = response.indexOf(':');
		final String userResponse = response.substring(colonPosition + 1).trim();
		return "".equals(userResponse) ? new String[0] : userResponse.split(",\\s+");
	}

	/**
	 * Close the communication channel to MineCraft. After that, no
	 * communication is possible anymore.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 */
	public void close() throws IOException {
		synchronized( syncObject ) {
			if( !socket.isClosed() ) {
				socket.close();
			}
		}
	}

	/**
	 * Revokes a player's operator status.
	 *
	 * @param player
	 * 		The player to be revoked.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void deOp(final String player) throws AuthenticationException, IOException {
		final String response = send("deop " + player);
		assert "".equals(response);
	}

	/**
	 * Changes the game mode for player to Survival mode or Creative mode.
	 * Remember, this will only affect player and no one else; it may confuse
	 * others.
	 * <p>
	 * Note: Player must currently be online for the command to work.
	 *
	 * @param player
	 * 		The player for which the game mode needs to be changed.
	 * @param mode
	 * 		The new mode for the player.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void gameMode(final String player, final GameMode mode) throws AuthenticationException, IOException {
		final String response = send("gamemode " + mode.getNumber());
		assert "".equals(response);
	}

	/**
	 * Spawn an item defined by data-value at player's location.
	 *
	 * @param player
	 * 		The player to give the item
	 * @param dataValue
	 * 		The data value to give.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void give(final String player, final int dataValue) throws AuthenticationException, IOException {
		give(player, dataValue, 1);
	}

	/**
	 * Spawns amount of the item defined by data-value at player's location.
	 *
	 * @param player
	 * 		The player to give the item
	 * @param dataValue
	 * 		The data value to give.
	 * @param amount
	 * 		The amount of items to give.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void give(final String player, final int dataValue, final int amount) throws AuthenticationException, IOException {
		give(player, dataValue, amount, 0);
	}

	/**
	 * Spawns amount of the item defined by data-value with the specified damage
	 * value at player's location.
	 *
	 * @param player
	 * 		The player to give the item
	 * @param dataValue
	 * 		The data value to give.
	 * @param amount
	 * 		The amount of items to give.
	 * @param damage
	 * 		The damage value.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void give(final String player, final int dataValue, final int amount, final int damage)
			throws AuthenticationException, IOException {
		final String response = send("give " + player + ' ' + dataValue + ' ' + amount + ' ' + damage);
		assert "".equals(response);
	}

	/**
	 * Forcibly disconnects player from the server.
	 *
	 * @param player
	 * 		The player to disconnect.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void kick(final String player) throws AuthenticationException, IOException {
		final String response = send("kick " + player);
		assert "".equals(response);
	}

	/**
	 * Shows the names of all currently connected players.
	 *
	 * @return A list with the currently connected players.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public List<String> list() throws IOException, AuthenticationException {
		final String response = send("list");
		final int colonPosition = response.indexOf(':');
		final String userResponse = response.substring(colonPosition + 1).trim();
		return "".equals(userResponse) ?
				new ArrayList<>() : Arrays.asList(userResponse.split(",\\s+"));
	}

	/**
	 * Grants player operator status on the server.
	 *
	 * @param player
	 * 		The player to be granted.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void op(final String player) throws AuthenticationException, IOException {
		final String response = send("op " + player);
		assert "".equals(response);
	}

	/**
	 * Removes player from the blacklist, allowing them to connect again.
	 *
	 * @param player
	 * 		The player to be removed.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void pardon(final String player) throws AuthenticationException, IOException {
		final String response = send("pardon " + player);
		assert "".equals(response);
	}

	/**
	 * Removes a host from the blacklist, allowing players from that host to
	 * connect to the server.
	 *
	 * @param host
	 * 		The host to be removed.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void pardonIp(final String host) throws AuthenticationException, IOException {
		final String response = send("pardon-ip " + host);
		assert "".equals(response);
	}

	/**
	 * Forces the server to write all pending changes to the world to disk.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void saveAll() throws AuthenticationException, IOException {
		final String response = send("save-all");
		assert "".equals(response);
	}

	/**
	 * Disables the server writing to the world files. All changes will
	 * temporarily be queued.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void saveOff() throws AuthenticationException, IOException {
		final String response = send("save-off");
		assert "".equals(response);
	}

	/**
	 * Enables the server writing to the world files. This is the default
	 * behavior.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void saveOn() throws AuthenticationException, IOException {
		final String response = send("save-on");
		assert "".equals(response);
	}

	/**
	 * Broadcasts message to all players on the server (in bright pink letters)
	 *
	 * @param message
	 * 		The message to broadcast.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void say(final String message) throws AuthenticationException, IOException {
		final String response = send("say " + message);
		assert "".equals(response);
	}

	/**
	 * Gracefully shuts down the server.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void stop() throws AuthenticationException, IOException {
		final String response = send("stop");
		assert "".equals(response);
	}

	/**
	 * Sends a message that only player sees. It will appear in the chat window
	 * in grey in the form of "yourname whispers message".
	 *
	 * @param player
	 * 		The player.
	 * @param message
	 * 		The message.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void tell(final String player, final String message)
			throws AuthenticationException, IOException {
		final String response = send("tell " + message);
		assert "".equals(response);
	}

	/**
	 * Add (or subtract) amount to the world time. Time is an integer between 0
	 * and 24000, inclusive, where 0 is dawn, 6000 midday, 12000 dusk and 18000
	 * midnight (i.e. the clock is bisected; left side is night, right side is
	 * day).
	 *
	 * @param amount
	 * 		The amount to add or subtract.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void timeAdd(final int amount)
			throws AuthenticationException, IOException {
		final String response = send("time add " + amount);
		assert "".equals(response);
	}

	/**
	 * Set the world time. time is an integer between 0 and 24000, inclusive,
	 * where 0 is dawn, 6000 midday, 12000 dusk and 18000 midnight (i.e. the
	 * clock is bisected; left side is night, right side is day).
	 *
	 * @param time
	 * 		The time to set.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void timeSet(final int time) throws AuthenticationException, IOException {
		final String response = send("time set " + time);
		assert "".equals(response);
	}

	/**
	 * Toggles rain and snow.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void toggleDownfall() throws AuthenticationException, IOException {
		final String response = send("toggledownfall");
		assert "".equals(response);
	}

	@Override
	public String toString() {
		return "RCon [requestId=" + requestId + ", socket=" + socket + "]";
	}

	/**
	 * Teleports player to targetPlayer 's location.
	 *
	 * @param player
	 * 		The player to teleport.
	 * @param targetPlayer
	 * 		The player to be teleported to.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void tp(final String player, final String targetPlayer) throws AuthenticationException, IOException {
		final String response = send("tp " + player + ' ' + targetPlayer);
		assert "".equals(response);
	}

	/**
	 * Displays all players in the whitelist.
	 *
	 * @return A list with the players on the whitelist.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public String[] whitelist() throws IOException, AuthenticationException {
		final String response = send("whitelist list");
		final int colonPosition = response.indexOf(':');
		final String userResponse = response.substring(colonPosition + 1).trim();

		return "".equals(userResponse) ? new String[0] : userResponse.split(",?\\s+");
	}

	/**
	 * Adds player to the whitelist.
	 *
	 * @param player
	 * 		The player to be added.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void whitelistAdd(final String player) throws IOException, AuthenticationException {
		final String response = send("whitelist add " + player);
		assert "".equals(response);
	}

	/**
	 * Disables the server's use of a whitelist.
	 * <p>
	 * Note: Server ops will always be able to connect when the whitelist is
	 * active, even if their names do not appear in the whitelist.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void whitelistOff() throws IOException, AuthenticationException {
		final String response = send("whitelist off");
		assert "".equals(response);
	}

	/**
	 * Enables the server's use of a whitelist.
	 * <p>
	 * Note: Server ops will always be able to connect when the whitelist is
	 * active, even if their names do not appear in the whitelist.
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void whitelistOn() throws IOException, AuthenticationException {
		final String response = send("whitelist on");
		assert "".equals(response);
	}

	/**
	 * Reloads the list of players in white-list.txt from disk (used when
	 * white-list.txt has been modified outside of MineCraft).
	 *
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void whitelistReload() throws IOException, AuthenticationException {
		final String response = send("whitelist reload");
		assert "".equals(response);
	}

	/**
	 * Remove player from the whitelist.
	 *
	 * @param player
	 * 		The player to be removed.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void whitelistRemove(final String player) throws IOException, AuthenticationException {
		final String response = send("whitelist remove " + player);
		assert "".equals(response);
	}

	/**
	 * Gives the specified user the given number of orbs. Maximum is 5000 per
	 * command. Negative amounts may be used to remove experience progress, but
	 * not actual levels.
	 *
	 * @param player
	 * 		The player to give the orbs.
	 * @param amount
	 * 		The amount of orbs to give.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws AuthenticationException
	 * 		The authentication using the password failed.
	 */
	public void xp(final String player, final int amount) throws AuthenticationException, IOException {
		final String response = send("xp " + player + ' ' + amount);
		assert "".equals(response);
	}

	/**
	 * Send data to MineCraft and return is't response.
	 *
	 * @param type
	 * 		The type of the packet to use.<br>
	 * 		COMMAND_TYPE: send a command packet.<br>
	 * 		LOGIN_TYPE: send a login packet.
	 * @param payload
	 * 		The data to send.
	 * @return The response.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws IncorrectRequestIdException
	 * 		The request id was not as expected.
	 */
	private byte[] send(final int type, final byte[] payload) throws IOException, IncorrectRequestIdException {
		final byte[] receivedPayload;
		synchronized( syncObject ) {
			// Send the command.
			final int sendLength = 4 + 4 + payload.length + 2;
			final byte[] sendBytes = new byte[4 + sendLength];
			final ByteBuffer sendBuffer = ByteBuffer.wrap(sendBytes);
			sendBuffer.order(ByteOrder.LITTLE_ENDIAN);
			sendBuffer.putInt(sendLength);
			sendBuffer.putInt(requestId);
			sendBuffer.putInt(type);
			sendBuffer.put(payload);
			sendBuffer.put((byte) 0).put((byte) 0);
			outputStream.write(sendBytes);
			outputStream.flush();

			// Receive the response.
			final byte[] receivedBytes = new byte[2048];
			final int receivedBytesLength = inputStream.read(receivedBytes);

			final ByteBuffer receivedBuffer = ByteBuffer.wrap(receivedBytes, 0, receivedBytesLength);
			receivedBuffer.order(ByteOrder.LITTLE_ENDIAN);
			final int receivedLength = receivedBuffer.getInt();
			final int receivedRequestId = receivedBuffer.getInt();
			@SuppressWarnings("unused")
			final int receivedType = receivedBuffer.getInt();
			receivedPayload = new byte[receivedLength - 4 - 4 - 2];
			receivedBuffer.get(receivedPayload);
			receivedBuffer.get(new byte[2]);
			if( receivedRequestId != requestId ) {
				throw new IncorrectRequestIdException(receivedRequestId);
			}
		}
		return receivedPayload;
	}

	/**
	 * Send data to MineCraft and return is't response.
	 *
	 * @param type
	 * 		The type of the packet to use.<br>
	 * 		COMMAND_TYPE: send a command packet.<br>
	 * 		LOGIN_TYPE: send a login packet.
	 * @param payload
	 * 		The data to send.
	 * @return The response.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws IncorrectRequestIdException
	 * 		The request id was not as expected.
	 */
	private String send(final int type, final String payload)
			throws IOException, IncorrectRequestIdException {
		return new String(send(type, payload.getBytes(StandardCharsets.US_ASCII)),
				StandardCharsets.US_ASCII);
	}

	/**
	 * Send a command to MineCraft and return is't response. This method will
	 * always send the command using the COMMAND_TYPE packet type.
	 *
	 * @param payload
	 * 		The data to send.
	 * @return The response.
	 * @throws IOException
	 * 		Some sort of I/O exception occurred.
	 * @throws IncorrectRequestIdException
	 * 		The request id was not as expected.
	 */
	private String send(final String payload)
			throws IOException, IncorrectRequestIdException {
		return send(COMMAND_TYPE, payload);
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

}
