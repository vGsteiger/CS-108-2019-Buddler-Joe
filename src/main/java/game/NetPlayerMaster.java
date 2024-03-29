package game;

import engine.render.Loader;
import engine.render.MasterRenderer;
import entities.NetPlayer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the current lobby on the client side. Gets updated via Packets and is fully static.
 */
public class NetPlayerMaster {

  private static final Logger logger = LoggerFactory.getLogger(NetPlayerMaster.class);

  private static String lobbyname;
  private static Map<Integer, NetPlayer> netPlayers;

  static {
    lobbyname = "";
    netPlayers = new ConcurrentHashMap<>();
  }

  /**
   * Initialize once at the start to load textures.
   *
   * @param loader main loader
   */
  public static void init(Loader loader) {}

  /**
   * Adds net players to render list. Call before rendering
   *
   * @param renderer master renderer instance
   */
  public static void update(MasterRenderer renderer) {

    for (NetPlayer netPlayer : netPlayers.values()) {
      if (!netPlayer.isDefeated()) {
        float pctBrightness = Game.getMap().getLightLevel(netPlayer.getPosition().y);
        if (pctBrightness > .7f) {
          netPlayer.turnHeadlightOff();
        } else {
          netPlayer.turnHeadlightOn();
        }
      }

      netPlayer.update();
      renderer.processEntity(netPlayer);
    }
  }

  /**
   * Adds a new player object that is linked to a client on the server. Will mainly be used by the
   * lobby overview packet that sends updates for new players.
   *
   * @param clientId server-side client id
   * @param username username of the player
   */
  public static void addPlayer(int clientId, String username) {
    //logger.info("adding " + username);
    if (!netPlayers.containsKey(clientId)) {
      NetPlayer newPlayer = new NetPlayer(clientId, username, new Vector3f(1000, 1000, 3), 0, 0, 0);
      netPlayers.put(clientId, newPlayer);
      // logger.info(NetPlayerMaster.staticToString());
    }
  }

  /**
   * Checks if a player left the lobby and removes the player from the game. Mainly called by the
   * lobby overview packet that will send a full list of all players.
   *
   * @param presentIds list of all currently connected (to the lobby) players by their client id
   */
  public static void removeMissing(ArrayList<Integer> presentIds) {
    // getIds().removeIf(netId -> !presentIds.contains(netId));
    for (Map.Entry<Integer, NetPlayer> netPlayer : netPlayers.entrySet()) {
      if (!presentIds.contains(netPlayer.getKey())) {
        // logger.debug("Removing " + netPlayer.getValue().getUsername());
        netPlayer.getValue().removeNameplate();
        netPlayers.remove(netPlayer.getKey());
      }
    }

    //logger.info(NetPlayerMaster.staticToString());
  }

  // public static void removePlayer(int clientId) {
  //  netPlayers.remove(clientId);
  //  System.out.println(NetPlayerMaster.staticToString());
  // }

  public static Set<Integer> getIds() {
    return netPlayers.keySet();
  }

  public static String getLobbyname() {
    return lobbyname;
  }

  public static void setLobbyname(String lobbyname) {
    NetPlayerMaster.lobbyname = lobbyname;
  }

  /**
   * String representation of the Game Lobby.
   *
   * @return string with all players in the lobby excluding the player
   */
  public static String staticToString() {
    StringJoiner sj = new StringJoiner(", ", "Players in my Lobby: ", "");
    for (NetPlayer netPlayer : netPlayers.values()) {
      sj.add(netPlayer.getUsername());
    }
    return sj.toString();
  }

  /**
   * Updates the position of a connected player. Called by {@link net.packets.playerprop.PacketPos}
   * to propagate player movement.
   *
   * @param clientId player to update
   * @param posX new X position
   * @param posY new Y position
   * @param rotY new Y rotation
   */
  public static void updatePosition(int clientId, float posX, float posY, float rotY) {
    NetPlayer netPlayer = netPlayers.get(clientId);
    if (netPlayer != null) {
      netPlayer.setPosition(new Vector3f(posX, posY, netPlayer.getPosition().z));
      netPlayer.setRotY(rotY);
    }
  }

  /**
   * Update the velocity vectors for a player. Called by the velocity packet.
   *
   * @param clientId player to update velocities for
   * @param curvX current X velocity
   * @param curvY current Y velocity
   * @param tarvX goal X velocity for interpolation
   * @param tarvY goal Y velocity for interpolation
   */
  public static void updateVelocities(
      int clientId, float curvX, float curvY, float tarvX, float tarvY) {
    NetPlayer netPlayer = netPlayers.get(clientId);
    if (netPlayer != null) {
      netPlayer.updateVelocities(new Vector3f(curvX, curvY, 0), new Vector3f(tarvX, tarvY, 0));
    }
  }

  /**
   * Returns the NetPlayer related to a clientId in this lobby. Will return a ServerPlayer object if
   * the clientId refers to the active player and will return null if the clientId is not found.
   *
   * @param clientId clientId for a player in this lobby.
   * @return The NetPlayer object related to the clientId
   */
  public static NetPlayer getNetPlayerById(int clientId) {
    try {
      if (clientId == Game.getActivePlayer().getClientId()) {
        return Game.getActivePlayer();
      } else {
        return netPlayers.get(clientId);
      }
    } catch (NullPointerException e) {
      logger.error("Player not existing");
      return null;
    }
  }

  public static Map<Integer, NetPlayer> getNetPlayers() {
    return netPlayers;
  }

  /** Reset NetPlayerMaster before a new game. This will also reset the static part of NetPlayer. */
  public static void reset() {
    lobbyname = "";
    netPlayers.clear();
    NetPlayer.reset();
  }
}
